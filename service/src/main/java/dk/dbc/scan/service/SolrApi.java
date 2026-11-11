/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of scan-service
 *
 * scan-service is free software: you can redistribute it and/or mapTo
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.Iterator;
import java.util.Optional;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.FieldAnalysisRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.AnalysisResponseBase;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class SolrApi {

    private static final Logger log = LoggerFactory.getLogger(SolrApi.class);

    @Inject
    Config config;

    public SolrApi() {
    }

    private SolrApi(Config config) {
        this.config = config;
    }

    static SolrApi instance(Config config) {
        return new SolrApi(config);
    }

    /**
     * Ask a SolR for the register value of this string
     *
     * @param fieldName  Name of the field
     * @param fieldValue Search string to mapTo
     * @return converted string
     * @throws SolrServerException If the SolR is down or the field-analysis
     *                             request is malformed
     * @throws IOException         If communication with the SolR fails
     */
    @Timed
    public String normalize(String fieldName, String fieldValue) throws SolrServerException, IOException {
        FieldAnalysisResponse resp = new FieldAnalysisRequest("/analysis/field")
                .setFieldNames(List.of(fieldName))
                .setFieldValue(fieldValue)
                .process(config.getSolrClient());
        FieldAnalysisResponse.Analysis analysis = resp
                .getFieldNameAnalysis(fieldName);
        if (analysis != null) {
            Iterator<AnalysisResponseBase.AnalysisPhase> iterator = analysis
                    .getIndexPhases()
                    .iterator();
            if (iterator.hasNext()) {
                AnalysisResponseBase.AnalysisPhase ap = iterator.next();
                while (iterator.hasNext()) {
                    ap = iterator.next();
                }
                List<AnalysisResponseBase.TokenInfo> tokens = ap.getTokens();
                if (tokens.size() == 1) {
                    return tokens.get(0).getText();
                }
            }
        }
        throw new SolrServerException("Cannot analyze text");

    }

    /**
     * Get a list of terms from a given register
     *
     * @param fieldName  Name of the field
     * @param fieldValue Normalized search string
     * @param cont       If the current term should be included in the result
     * @param count      Number of terms to get
     * @param trackingId For tracking of requests
     * @return List of terms returned from SolR
     * @throws SolrServerException If the SolR is down or the terms request is
     *                             malformed
     * @throws IOException         If communication with the SolR fails
     */
    @Timed
    public List<String> scan(String fieldName, String fieldValue, boolean cont, int count, String trackingId) throws SolrServerException, IOException {
        ModifiableSolrParams params = new SolrQuery()
                .set("terms.sort", "index")
                .set("terms.fl", fieldName)
                .set("terms.lower", fieldValue)
                .set("terms.lower.incl", !cont)
                .set("terms.limit", count)
                .set("terms.raw", false)
                .set("trackingId", trackingId)
                .set("appId", config.getAppId());
        QueryResponse resp = new QueryRequest("/terms", params)
                .process(config.getSolrClient());
        if (resp.getStatus() != 0) {
            log.warn("Error in request (terms): {} = {}", params, resp);
            throw new SolrServerException("terms: error in request");
        }
        List<TermsResponse.Term> terms = resp.getTermsResponse().getTerms(fieldName);
        return terms.stream()
                .map(TermsResponse.Term::getTerm)
                .collect(toList());
    }

    /**
     *
     * @param fieldName   Name of the field
     * @param fieldValue  Normalized search string (from terms)
     * @param filterQuery profile restrictions
     * @return number of hits in said profile
     * @throws SolrServerException If the SolR is down or the select request is
     *                             malformed
     * @throws IOException         If communication with the SolR fails
     */
    @Timed
    public long getHitCount(String fieldName, String fieldValue, String filterQuery) throws SolrServerException, IOException {
        SolrQuery req = new SolrQuery()
                .setQuery(fieldName + ":" + ClientUtils.escapeQueryChars(fieldValue))
                .setFilterQueries(filterQuery)
                .setParam("appId", config.getAppId())
                .setRows(0);
        QueryResponse resp = config.getSolrClient().query(req);
        if (resp.getStatus() != 0) {
            log.warn("Error in request (select): {} = {}", req, resp);
            throw new SolrServerException("select: error in request");
        }
        return resp.getResults().getNumFound();
    }

    private static final Pattern ZK = Pattern.compile("zk://([^/]*)(/.*)?/([^/]*)");

    /**
     * Make a SolrClient
     *
     * @param solrUrl http or zk url to a SolR
     * @return client
     */
    public static SolrClient makeSolrClient(String solrUrl) {
        Matcher zkMatcher = ZK.matcher(solrUrl);
        if (zkMatcher.matches()) {
            Optional<String> zkChroot = Optional.empty();
            if (zkMatcher.group(2) != null) {
                zkChroot = Optional.of(zkMatcher.group(2));
            }
            List<String> zkHosts = List.of(zkMatcher.group(1).split(","));
            return new CloudHttp2SolrClient.Builder(zkHosts, zkChroot)
                    .withDefaultCollection(zkMatcher.group(3))
                    .build();
        } else {
            return new Http2SolrClient.Builder(solrUrl)
                    .build();
        }
    }

}
