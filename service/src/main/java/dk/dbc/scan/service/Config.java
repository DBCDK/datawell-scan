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
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    private int maxCount;
    private int parallelHitcountRequests;
    private Client httpClient;
    private UriBuilder vipCore;

    public Config() {
        this.env = System.getenv();
    }

    public Config(String... env) {
        this.env = Stream.of(env)
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
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
        this.maxCount = Integer.parseUnsignedInt(get("MAX_COUNT", "100"));
        if (maxCount <= 0)
            throw new IllegalArgumentException("variable MAX_COUNT should be at least 1");
        this.parallelHitcountRequests = Integer.parseUnsignedInt(get("PARALLEL_HITCOUNT_REQUESTS", "20"));
        if (parallelHitcountRequests <= 0)
            throw new IllegalArgumentException("variable PARALLEL_HITCOUNT_REQUESTS should be at least 1");
        vipCore = UriBuilder.fromPath(get("VIPCORE_ENDPOINT"));
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

    public int getMaxCount() {
        return maxCount;
    }

    public int getParallelHitcountRequests() {
        return parallelHitcountRequests;
    }

    public UriBuilder getVipCore() {
        return vipCore.clone();
    }

    public Client getVipCoreHttpClient(String trackingId) {
        return getHttpClient()
                .register((ClientRequestFilter) (ClientRequestContext context) ->
                        context.getHeaders().putSingle("X-DBCTrackingId", trackingId));
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
