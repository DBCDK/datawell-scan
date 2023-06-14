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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dk.dbc.vipcore.marshallers.ProfileServiceResponse;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class ProfileServiceCache {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceCache.class);
    private static final ObjectMapper O = JsonMapper
            .builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .build();

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
        log.debug("filterQueryFor called with agency {} and profile {}", agencyId, profile);
        URI uri = config.getVipCore().path("profileservice/search/{agencyId}/{profile}").build(agencyId, profile);
        try (InputStream is = config.getVipCoreHttpClient(trackingId)
                             .target(uri)
                             .request(MediaType.APPLICATION_JSON)
                             .get(InputStream.class))
        {
            ProfileServiceResponse resp = O.readValue(is, ProfileServiceResponse.class);
            if (resp.getError() != null) {
                log.warn("Got an error: {} for agency {} and profile {}", resp.getError().value(), agencyId, profile);
                throw new ServerErrorException(resp.getError().value(), jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
            }
            final String res = resp.getFilterQuery();
            return res;
        } catch (JsonParseException e) {
            log.warn("Error occurred when fetching filter query for agency {}, profile {}: {}", agencyId, profile, e.getMessage());
            throw new ServerErrorException(e.getMessage(), jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
