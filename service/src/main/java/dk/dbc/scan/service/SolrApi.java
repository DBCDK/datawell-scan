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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
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
        ModifiableSolrParams req = new SolrQuery()
                .setRequestHandler("/analysis/field")
                .set("analysis.fieldname", fieldName)
                .set("analysis.fieldvalue", fieldValue)
                .set("appId", config.getAppId());
        QueryResponse resp = config.getSolrClient().query(req);

        return Checker.of(resp)
                .raises(o -> {
                    log.warn("Error in response (FieldAnalysis): {}", o);
                    return new SolrServerException("FieldAnalysis: malformed response");
                })
                .ensure(o -> o.getStatus() == 0)
                .mapTo(o -> o.getResponse())
                .ensure(o -> o.findRecursive("responseHeader", "status").equals(0))
                .mapTo(o -> o.findRecursive("analysis", "field_names", fieldName, "index"))
                .as(NamedList.class)
                .ensure(o -> o.size() != 0) // Non-empty tokenizer/filter list
                .mapTo(o -> o.getVal(o.size() - 1)) // last tokenizer/filter
                .as(List.class)
                .ensure(o -> o.size() == 1) // has not been tokenized
                .mapTo(o -> o.get(0)) // get only element
                .as(NamedList.class)
                .mapTo(o -> o.get("text")) // "text" is tokenized input
                .as(String.class)
                .get();
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
        ModifiableSolrParams req = new SolrQuery()
                .setRequestHandler("/terms")
                .set("terms.sort", "index")
                .set("terms.fl", fieldName)
                .set("terms.lower", fieldValue)
                .set("terms.lower.incl", !cont)
                .set("terms.limit", count)
                .set("terms.raw", false)
                .set("trackingId", trackingId)
                .set("appId", config.getAppId());
        QueryResponse resp = config.getSolrClient().query(req);
        if (resp.getStatus() != 0) {
            log.warn("Error in request (terms): {} = {}", req, resp);
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
            List<String> zkHosts = Arrays.asList(zkMatcher.group(1).split(","));
            CloudSolrClient solrClient = new CloudSolrClient.Builder(zkHosts, zkChroot)
                    .build();

            solrClient.setDefaultCollection(zkMatcher.group(3));

            return solrClient;
        } else {
            return new Http2SolrClient.Builder(solrUrl)
                    .build();
        }
    }

}
