/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-profile-change-update
 *
 * datawell-scan-profile-change-update is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-profile-change-update is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.update;

import dk.dbc.scan.common.Profile;
import dk.dbc.scan.common.ProfileDB;
import dk.dbc.scan.common.ProfileServiceActions;
import dk.dbc.scan.common.ReThrowException;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class UpdateIT extends DB {

    @Test(timeout = 10_000L)
    public void testUpdate() throws Exception {
        System.out.println("testUpdate");

        addDocuments(
                d -> d.manifestationId("870970-basis:a")
                        .collectionIdentifier("102030-foo"),
                d -> d.manifestationId("870970-basis:b")
                        .collectionIdentifier("102030-bar"), // remove by collectionIdentifier
                d -> d.manifestationId("870970-basis:c")
                        .collectionIdentifier("102030-any"),
                d -> d.manifestationId("870970-basis:d")
                        .holdingsAgencyId("777777"), // add by holdingAgencyId
                d -> d.manifestationId("700000-katalog:e")
                        .collectionIdentifier("700000-katalog") // add by collectionIdentifier
        );

        ProfileDB profileDB = new ProfileDB(profileUrl);
        SolrApi solrApi = new SolrApi(solrUrl);
        SolrDocStoreDB solrDocStoreDB = new SolrDocStoreDB(solrDocStoreUrl);
        Profile profile = new Profile(asList("102030-foo",
                                             "102030-bar"));
        profileDB.updateProfiles(profileDB.readProfiles(), new HashMap<String, Profile>() {
                             {
                                 put("777777-me", profile);
                             }
                         });
        Update update = new Update() {
            @Override
            ProfileDB createProfileDb(Arguments arguments) {
                return profileDB;
            }

            @Override
            ProfileServiceActions createProfileServiceActions(Arguments arguments) {
                return ReThrowException.wrap(() -> {
                    ProfileServiceActions m = mock(ProfileServiceActions.class);
                    when(m.getProfiles(anyList())).thenCallRealMethod();
                    when(m.getProfile("777777-me"))
                            .thenReturn(profile);
                    return m;
                });
            }

            @Override
            SolrApi createSolrApi(Arguments arguments) {
                return solrApi;
            }

            @Override
            SolrDocStoreDB createSolrDocStoreDb(Arguments arguments) {
                return solrDocStoreDB;
            }
        };
        Arguments arguments = mock(Arguments.class);
        when(arguments.getQueues())
                .thenReturn(asList("abc"));
        when(arguments.getBatchSize())
                .thenReturn(10);
        when(arguments.getProfiles())
                .thenReturn(asList("777777-me"));
        when(arguments.hasQueue())
                .thenReturn(true);

        profile.add("777777-katalog"); // holdings agency add
        profile.add("700000-katalog"); // collection identifier add
        profile.remove("102030-bar");  // collection identifier remove

        listQueue();

        boolean ok = update.run(arguments);
        ArrayList<String> queued = listQueue();

        System.out.println("ok = " + ok);
        System.out.println("queued = " + queued);

        assertThat(ok, is(true));
        assertThat(queued, containsInAnyOrder("870970-basis:b",
                                              "870970-basis:d",
                                              "700000-katalog:e"));
    }

    private ArrayList<String> listQueue() throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        try (Connection connction = solrDocStoreDs.getConnection() ;
             Statement stmt = connction.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("DELETE FROM queue RETURNING jobId")) {
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
        }
        return list;
    }
}
