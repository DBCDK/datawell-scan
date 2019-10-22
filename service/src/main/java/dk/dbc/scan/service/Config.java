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

import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.UriBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@ApplicationScoped
@Singleton
@Startup
@Lock(LockType.READ)
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private final Map<String, String> env;

    private String appId;
    private SolrClient solrClient;
    private UriBuilder profileService;
    private int maxCount;
    private int parallelHitcountRequests;
    private Client httpClient;

    public Config() {
        this.env = System.getenv();
    }

    static Config instance() {
        Config that = new Config();
        that.init();
        return that;
    }

    @PostConstruct
    public void init() {
        appId = get("SOLR_APPID");
        String userAgent = get("USER_AGENT", "ScanService/0.1");
        log.debug("Using: {} as HttpUserAgent", userAgent);
        this.httpClient = ClientBuilder.newBuilder()
                .register((ClientRequestFilter) (ClientRequestContext context) ->
                        context.getHeaders().putSingle("User-Agent", userAgent)
                )
                .build();

        String solrUrl = get("SOLR_URL");
        this.solrClient = SolrApi.makeSolrClient(solrUrl);
        this.profileService = UriBuilder.fromUri(get("PROFILE_SERVICE_URL"))
                .path("api/profile/{agencyId}/{profile}")
                .queryParam("trackingId", "{trackingId}");
        this.maxCount = Integer.parseUnsignedInt(get("MAX_COUNT", "100"));
        if (maxCount <= 0)
            throw new IllegalArgumentException("variable MAX_COUNT should be atleast 1");
        this.parallelHitcountRequests = Integer.parseUnsignedInt(get("PARALLEL_HITCOUNT_REQUESTS", "20"));
        if (parallelHitcountRequests <= 0)
            throw new IllegalArgumentException("variable PARALLEL_HITCOUNT_REQUESTS should be atleast 1");
    }

    public String getAppId() {
        return appId;
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public SolrClient getSolrClient() {
        return solrClient;
    }

    public UriBuilder getProfileService() {
        return profileService.clone();
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getParallelHitcountRequests() {
        return parallelHitcountRequests;
    }

    private String get(String key) {
        String val = env.get(key);
        if (val == null)
            throw new IllegalArgumentException("Missing required variable: " + key);
        return val;
    }

    private String get(String key, String fallback) {
        return env.getOrDefault(key, fallback);
    }

}
