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
package dk.dbc.scan.update;

import dk.dbc.pgqueue.BatchQueueSupplier;
import dk.dbc.pgqueue.QueueSupplier;
import dk.dbc.scan.common.Profile;
import dk.dbc.scan.common.ProfileDB;
import dk.dbc.scan.common.ProfileServiceActions;
import dk.dbc.scan.common.ReThrowException;
import dk.dbc.search.solrdocstore.queue.QueueJob;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class Update {

    private static final Logger log = LoggerFactory.getLogger(Update.class);

    public boolean run(Arguments arguments) throws IOException, SQLException {
        HashMap<String, Profile> currentProfiles = new HashMap<>();

        ProfileServiceActions profileService = createProfileServiceActions(arguments);
        ProfileDB profileDb = createProfileDb(arguments);

        HashSet<String> profiles = new HashSet<>(arguments.getProfiles());
        Map<String, Profile> storedProfiles = profileDb.readProfiles();
        if (arguments.hasImportProfile())
            profiles.addAll(storedProfiles.keySet());
        for (String profileName : profiles) {
            Profile profile = profileService.getProfile(profileName);
            currentProfiles.put(profileName, profile);
        }

        if (!arguments.isOnlySyncDatabase()) {
            SolrDocStoreDB solrDocStoreDb = createSolrDocStoreDb(arguments);
            SolrApi solrApi = createSolrApi(arguments);
            List<String> queues = arguments.hasQueue() ? arguments.getQueues() : solrDocStoreDb.getQueues();
            int batchSize = arguments.getBatchSize();
            if (!requeue(storedProfiles, currentProfiles, solrDocStoreDb, solrApi, queues, batchSize))
                return false;
        }
        log.info("Updating database to reflect current profiles");
        profileDb.updateProfiles(storedProfiles, currentProfiles);
        return true;
    }

    private boolean requeue(Map<String, Profile> storedProfiles, HashMap<String, Profile> currentProfiles, SolrDocStoreDB solrDocStoreDb, SolrApi solrApi, List<String> queues, int batchSize) throws IOException, SQLException {

        HashSet<String> collectionIdentifiers = new HashSet<>();
        HashSet<String> holdingsAgencies = new HashSet<>();
        Profile.allAffectedCollections(storedProfiles, currentProfiles, collectionIdentifiers, holdingsAgencies);
        if (collectionIdentifiers.isEmpty() && holdingsAgencies.isEmpty()) {
            System.err.println("No changes was identified!?!");
            return false;
        }
        log.info("Requeueing for:");
        collectionIdentifiers.stream()
                .sorted()
                .forEach(c -> System.out.println("  collection-identifier: " + c));
        holdingsAgencies.stream()
                .sorted()
                .forEach(a -> System.out.println("  holdings-for-agency: " + a));
        log.info("Requeueing to:");
        queues.stream()
                .sorted()
                .forEach(q -> System.out.println("  queue: " + q));
        String query = SolrApi.queryFrom(collectionIdentifiers, holdingsAgencies);
        log.debug("using query: {}", query);
        try (Connection connection = solrDocStoreDb.getConnection() ;
             BatchQueueSupplier<QueueJob> supplier = new QueueSupplier<>(QueueJob.STORAGE_ABSTRACTION)
                     .batchSupplier(connection, batchSize)) {
            connection.setAutoCommit(true);

            solrApi.manifestationIdStreamOf(query)
                    .map(ManifestationId::asQueueJob)
                    .forEach(job ->
                            queues.forEach(queue ->
                                    ReThrowException.wrap(() -> {
                                        log.debug("{} to {}", job, queue);
                                        supplier.enqueue(queue, job);
                                    })));
        } catch (ReThrowException ex) {
            ex.throwAs(SQLException.class);
        }
        return true;
    }

    //// For overriding in unittest
    SolrApi createSolrApi(Arguments arguments) {
        return new SolrApi(arguments.getSolrUrl());
    }

    ProfileDB createProfileDb(Arguments arguments) {
        ProfileDB profileDB = new ProfileDB(arguments.getProfileDb());
        profileDB.migrate();
        return profileDB;
    }

    SolrDocStoreDB createSolrDocStoreDb(Arguments arguments) {
        return new SolrDocStoreDB(arguments.getSolrDocStoreDb());
    }

    ProfileServiceActions createProfileServiceActions(Arguments arguments) {
        return new ProfileServiceActions(arguments.getVipCoreEndpoint());
    }

}
