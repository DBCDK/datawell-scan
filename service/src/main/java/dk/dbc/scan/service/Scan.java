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

import dk.dbc.log.LogWith;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.solr.client.solrj.SolrServerException;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
@Path("scan")
public class Scan {

    private static final Logger log = LoggerFactory.getLogger(Scan.class);
    private static final Pattern ALPHA_NUM = Pattern.compile("\\w+");

    @Inject
    Config config;

    @Inject
    ScanLogic scanLogic;

    public Scan() {
    }

    private Scan(Config config, ScanLogic scanLogic) {
        this.config = config;
        this.scanLogic = scanLogic;
    }

    static Scan instance(Config config, ScanLogic scanLogic) {
        return new Scan(config, scanLogic);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public ScanResponse.Success scan(@QueryParam("agencyId") Integer agencyIdNum,
                                     @QueryParam("profile") String profile,
                                     @QueryParam("term") String term,
                                     @QueryParam("register") String register,
                                     @QueryParam("count") @DefaultValue("10") int count,
                                     @QueryParam("continue") @DefaultValue("false") boolean cont,
                                     @QueryParam("trackingId") String trackingId) {
        if (trackingId == null || trackingId.isEmpty())
            trackingId = UUID.randomUUID().toString();
        try (LogWith mdc = LogWith.track(trackingId)) {
            ScanResponse.Request requestParam = new ScanResponse.Request(agencyIdNum, profile, term, register, count, cont, trackingId);
            try {
                if (agencyIdNum == null)
                    throw new IllegalArgumentException("Required parameter: agencyId is missing");
                if (profile == null || profile.isEmpty())
                    throw new IllegalArgumentException("Required parameter: profile is missing");
                if (!ALPHA_NUM.matcher(profile).matches())
                    throw new IllegalArgumentException("Required parameter: profile contains invalid characters");
                if (term == null)
                    throw new IllegalArgumentException("Required parameter: term is missing");
                if (register == null || register.isEmpty())
                    throw new IllegalArgumentException("Required parameter: register is missing");
                if (count <= 0)
                    throw new IllegalArgumentException("Parameter: count needs to be a positive number");
                count = Integer.min(count, config.getMaxCount());

                String agencyId = String.format("%06d", agencyIdNum);
                ScanResponse.Result result = scanLogic.scan(register, term, cont, count, agencyId, profile, trackingId);
                return new ScanResponse.Success(requestParam, result);

            } catch (IllegalArgumentException ex) {
                throw failure(ex.getMessage(), requestParam, Response.Status.BAD_REQUEST);
            } catch (WebApplicationException ex) {
                throw ex;
            } catch (SolrServerException | IOException | InterruptedException | RuntimeException ex) {
                log.error("Error processing request: {}", ex.getMessage());
                log.debug("Error processing request: ", ex);
                throw failure("Internal server error", requestParam, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
    }

    public WebApplicationException failure(String message, ScanResponse.Request requestParam, Response.StatusType status) {
        ScanResponse.Failure failure = new ScanResponse.Failure(message, requestParam);
        return new WebApplicationException(Response.status(status).entity(failure).build());
    }

}
