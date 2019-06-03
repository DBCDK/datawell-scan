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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class ProfileServiceCache {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceCache.class);
    private static final ObjectMapper O = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Inject
    Config config;

    public ProfileServiceCache() {
    }

    private ProfileServiceCache(Config config) {
        this.config = config;
    }

    static ProfileServiceCache instance(Config config) {
        return new ProfileServiceCache(config);
    }

    /**
     * Lookup queryfilter for a given profile in profile-service
     *
     * @param agencyId   Agency with a profile
     * @param profile    Name of profile
     * @param trackingId Tracking
     * @return Query filter as string
     * @throws IOException If communication with the service fails
     */
    @Timed
    @CacheResult(cacheName = "profileService",
                 exceptionCacheName = "profileServiceError",
                 cachedExceptions = {ClientErrorException.class,
                                     ServerErrorException.class,
                                     IOException.class})
    public String filterQueryFor(@CacheKey String agencyId, @CacheKey String profile, String trackingId) throws IOException {

        HashMap<String, Object> params = new HashMap<String, Object>() {
            {
                put("agencyId", agencyId);
                put("profile", profile);
                put("trackingId", trackingId);
            }
        };
        URI uri = config.getProfileService()
                .buildFromMap(params);

        log.debug("fetching profile: {}", uri);
        try (InputStream is = config.getHttpClient()
                .target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildGet()
                .invoke(InputStream.class)) {
            Response resp = O.readValue(is, Response.class);
            if (resp.success)
                return resp.filterQuery;
            log.warn("Got an error: {} for: {}", resp.error, uri);
            throw new ServerErrorException(resp.error, javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DTO for the profile-service call
     */
    private static class Response {

        public boolean success;
        public String error;
        public String filterQuery;
    }
}
