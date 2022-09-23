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

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ProfileDBIT extends IntegrationTestBase {

    @Test(timeout = 2_000L)
    public void testReadProfiles() throws Exception {
        System.out.println("testReadProfiles");
        ProfileDB profileDb = new ProfileDB(PG_URL);
        clearDb();
        addToDb("123456-abc", "123456-katalog", "870970-basis");
        addToDb("654321-abc", "777777-katalog", "870970-basis");

        Map<String, Profile> profiles = profileDb.readProfiles();
        System.out.println("profiles = " + profiles);

        assertThat(profiles.keySet(), containsInAnyOrder("123456-abc", "654321-abc"));
        assertThat(profiles.get("123456-abc"), containsInAnyOrder("123456-katalog", "870970-basis"));
        assertThat(profiles.get("654321-abc"), containsInAnyOrder("777777-katalog", "870970-basis"));
    }

    @Test(timeout = 2_000L)
    public void testUpdateProfiles() throws Exception {
        System.out.println("testUpdateProfiles");
        ProfileDB profileDb = new ProfileDB(PG_URL);
        clearDb();
        addToDb("123456-abc", "123456-katalog", "870970-basis");
        addToDb("654321-abc", "777777-katalog", "870970-basis");
        addToDb("876543-gut", "777777-katalog", "870970-basis"); // Should be removed
        Map<String, Profile> profilesOld = profileDb.readProfiles();
        Map<String, Profile> profilesNew = new HashMap<String, Profile>() {
            {
                put("123456-abc", new Profile("123456-katalog", "870970-basis"));
                put("654321-abc", new Profile("777777-katalog"));
                put("234567-foo", new Profile("870970-basis"));
            }
        };

        profileDb.updateProfiles(profilesOld, profilesNew);

        Map<String, Profile> profiles = profileDb.readProfiles();
        System.out.println("profiles = " + profiles);

        assertThat(profiles, is(profilesNew));
    }

    public void clearDb() throws SQLException {
        try (Connection connection = PG.createConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("TRUNCATE profiles");
        }
    }

    public void addToDb(String profile, String... collectionIdentifiers) throws SQLException {
        try (Connection connection = PG.createConnection() ;
             PreparedStatement stmt = connection.prepareStatement("INSERT INTO profiles(agencyId, classifier, collectionIdentifier) VALUES(?, ?, ?)")) {
            String[] parts = profile.split("-", 2);
            stmt.setInt(1, Integer.parseInt(parts[0]));
            stmt.setString(2, parts[1]);
            for (String collectionIdentifier : collectionIdentifiers) {
                stmt.setString(3, collectionIdentifier);
                stmt.executeUpdate();
            }
        }
    }
}
