/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-profile-change-monitor
 *
 * datawell-scan-profile-change-monitor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-profile-change-monitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import dk.dbc.vipcore.marshallers.ProfileServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ProfileServiceActions {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceActions.class);

    private static final ObjectMapper O = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Client client;
    private final UriBuilder uriBuilder;

    public ProfileServiceActions(String vipCore) {
        client = ClientBuilder.newBuilder()
                .register((ClientRequestFilter) (ClientRequestContext context) ->
                        context.getHeaders().putSingle("User-Agent", "ProfileServiceMonitor/1.0"))
                .build();
        this.uriBuilder = UriBuilder.fromUri(vipCore)
                .path("profileservice/search/{agencyId}/{profileName}");
    }

    public URI getUri(int agencyId, String profileName) {
        return uriBuilder.clone()
                .buildFromMap(new HashMap<String, Object>() {
                    {
                        put("agencyId", agencyId);
                        put("profileName", profileName);
                    }
                });
    }

    public Profile getProfile(String profile) throws IOException {
        String[] parts = profile.split("-", 2);
        URI uri = getUri(Integer.parseInt(parts[0]), parts[1]);
        try (InputStream is = client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(InputStream.class))
        {
            ProfileServiceResponse resp = O.readValue(is, ProfileServiceResponse.class);
            if (resp.getError() != null) {
                log.warn("Got an error: {} for: {}", resp.getError().value(), uri);
                throw new ServerErrorException(resp.getError().value(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
            }
            return new Profile(resp.getCollectionIdentifiers());
        }
    }

    public Map<String, Profile> getProfiles(List<String> profileNames) throws IOException {
        HashMap<String, Profile> collection = new HashMap<>();
        for (String profileName : profileNames) {
            collection.put(profileName, getProfile(profileName));
        }
        return collection;
    }
}
