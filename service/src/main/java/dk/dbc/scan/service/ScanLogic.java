/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of scan-service
 *
 * scan-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * scan-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class ScanLogic {

    private static final Logger log = LoggerFactory.getLogger(ScanLogic.class);

    private static final Iterator EMPTY_ITERATOR = Collections.EMPTY_LIST.iterator();

    @Inject
    Config config;

    @Inject
    ProfileServiceCache psCache;

    @Inject
    SolrApi solr;

    @Resource(type = ManagedExecutorService.class)
    ExecutorService mes;

    AtomicInteger hitCountInFlight;
    List<ScanResponse.Term> termsFound;

    public ScanLogic() {
    }

    private ScanLogic(Config config, ProfileServiceCache psCache, SolrApi solr, ExecutorService mes) {
        this.config = config;
        this.psCache = psCache;
        this.solr = solr;
        this.mes = mes;
    }

    static ScanLogic instance(Config config, ProfileServiceCache psCache, SolrApi solr, ExecutorService mes) {
        return new ScanLogic(config, psCache, solr, mes);
    }

    /**
     * Callback when an asynchronous call is done
     */
    private synchronized void inFlightDone() {
        hitCountInFlight.decrementAndGet();
        notify();
    }

    /**
     * Wait for all asynchronous calls to be completed
     *
     * @throws InterruptedException If the thread is interrupted
     */
    private synchronized void awaitAllInFlight() throws InterruptedException {
        while (hitCountInFlight.get() != 0) {
            wait();
        }
    }

    /**
     * Get the number of terms that are valid for the result
     *
     * @return number of terms that are ready to be returned
     */
    private int goodTermCount() {
        return (int) termsFound.stream()
                .filter(ScanResponse.Term::hasTerms)
                .count();
    }

    /**
     * Build result for scan
     *
     * @param register   The register to scan in
     * @param term       The term to scan with
     * @param cont       If current term shouldn't be included. This has the
     *                   side-effect that if term is included, then it is normalized, by solr
     *                   using a field-analysis request
     * @param count      Number of terms wanted in result
     * @param agencyId   Agency that performs the request
     * @param profile    The profile that is used by the agency
     * @param trackingId Tracking
     * @return Response to end-user
     * @throws IOException          In case of communication errors with solr
     * @throws SolrServerException  In case of request syntax errors or invalid
     *                              register name
     * @throws InterruptedException If something is timing out, or the process
     *                              is being shut down
     */
    public ScanResponse.Result scan(String register, String term, boolean cont, int count, String agencyId, String profile, String trackingId) throws IOException, SolrServerException, InterruptedException {
        String scanRegister = register + "_" + agencyId + "_" + profile;
        hitCountInFlight = new AtomicInteger(0);
        termsFound = new ArrayList<>();

        if (!cont) {
            term = solr.normalize(register, term);
            log.debug("normalized = {}", term);
        }

        String filterQuery = psCache.filterQueryFor(agencyId, profile, trackingId);
        log.debug("filterQuery = {}", filterQuery);

        Iterator<String> terms = EMPTY_ITERATOR;
        while (goodTermCount() < count) {
            if (!terms.hasNext()) {
                if (term == null) {// We've reached end of index
                    log.debug("We've reached the end of the index");
                    break;
                }
                int toGo = count - goodTermCount();
                int fetch = toGo + 5 + toGo / 8; // extra terms to fetch, so that zero hits can be skipped
                log.debug("fetch: {} new terms from {}", fetch, term);
                List<String> scan = solr.scan(scanRegister, term, cont, fetch, trackingId);
                cont = true;
                term = scan.size() != fetch ? null : scan.get(fetch - 1);
                terms = scan.iterator();
            }
            while (terms.hasNext()) {
                log.trace("chould we check hitcount");
                int goodTermCount = goodTermCount();
                log.trace("goodTermCount = {}", goodTermCount);
                if (goodTermCount >= count)
                    break;
                int toCheck = count - goodTermCount;
                log.trace("toGo = {}", toCheck);
                int notChecked = toCheck - hitCountInFlight.get();
                log.trace("notChecked = {}", notChecked);
                int toQueue = notChecked + 3; // overhead
                log.trace("toQueue = {}", toQueue);
                if (toQueue <= 0)
                    break;

                ScanResponse.Term checkTerm = new ScanResponse.Term(terms.next());
                termsFound.add(checkTerm);
                log.debug("added checkTerm = {}", checkTerm);
                hitCountInFlight.incrementAndGet();
                mes.submit(() -> {
                    try {
                        long hitcount = solr.getHitCount(register, checkTerm.getTerm(), filterQuery);
                        checkTerm.setCount(hitcount);
                        log.debug("changed checkTerm = {}", checkTerm);
                        inFlightDone();
                    } catch (SolrServerException | IOException ex) {
                        log.error("Error checking real hit count for: {}: {}", checkTerm.getTerm(), ex.getMessage());
                        log.debug("Error checking real hit count for: {}: ", checkTerm.getTerm(), ex);
                    }
                });
            }
        }
        awaitAllInFlight();
        log.debug("all: termsFound = {}", termsFound);

        List<ScanResponse.Term> allResponseTerms = termsFound.stream()
                .filter(ScanResponse.Term::hasTerms)
                .collect(Collectors.toList());
        int totalHitValidated = allResponseTerms.size();
        int validTermCount = Integer.min(count, totalHitValidated);
        List<ScanResponse.Term> responseTerms = allResponseTerms.subList(0, validTermCount);
        // To get data to tune the number of infligt hitcount requests in overhead
        log.info("Extra terms hit validated: {}", totalHitValidated - validTermCount);
        log.debug("responseTerms = {}", responseTerms);
        String continueAfter = validTermCount != count ? null : responseTerms.get(count - 1).getTerm();
        return new ScanResponse.Result(continueAfter, responseTerms);
    }
}
