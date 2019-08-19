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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ProfileDB extends GenericPostgreSQL {

    private static final Logger log = LoggerFactory.getLogger(ProfileDB.class);

    public ProfileDB(String url) {
        super(url);
    }

    public void migrate() {
        migrate(this);
    }

    public static void migrate(DataSource ds) {
        Flyway flyway = Flyway.configure()
                .baselineOnMigrate(true)
                .dataSource(ds)
                .locations("db/migrate")
                .load();
        for (MigrationInfo i : flyway.info().applied()) {
            log.debug("db task {} : {} from file '{}' (applied)", i.getVersion(), i.getDescription(), i.getScript());
        }
        for (MigrationInfo i : flyway.info().pending()) {
            log.debug("db task {} : {} from file '{}' (apply)", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();
    }

    /**
     * Get all the profiles from the database
     * <p>
     * Create a connection and read the profiles from it.
     *
     * @return a map of profile-name to collectionIdentifiers
     * @throws SQLException If the database acts up
     */
    public Map<String, Profile> readProfiles() throws SQLException {
        Map<String, Profile> profiles = new HashMap<>();
        try (Connection connection = getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT agencyId, classifier, collectionIdentifier FROM profiles")) {
            while (resultSet.next()) {
                String profile = new StringBuilder()
                        .append(resultSet.getInt(1))
                        .append("-")
                        .append(resultSet.getString(2))
                        .toString();
                profiles.computeIfAbsent(profile, s -> new Profile())
                        .add(resultSet.getString(3));
            }
        }
        return profiles;
    }

    /**
     * Update the database to reflect profile changes
     *
     * @param before How profiles are in the database
     * @param after  How they're supposed to be after the transformation
     * @throws SQLException If the database acts up
     */
    public void updateProfiles(Map<String, Profile> before, Map<String, Profile> after) throws SQLException {
        try (Connection connection = getConnection() ;
             PreparedStatement add = connection.prepareStatement("INSERT INTO profiles(agencyId, classifier, collectionIdentifier) VALUES(?, ?, ?)") ;
             PreparedStatement del = connection.prepareStatement("DELETE FROM profiles WHERE agencyId = ? AND classifier = ? AND collectionIdentifier = ?")) {
            connection.setAutoCommit(false);
            try {

                HashSet<String> allProfileNames = new HashSet<>();
                allProfileNames.addAll(before.keySet());
                allProfileNames.addAll(after.keySet());

                for (String profileName : allProfileNames) {
                    String[] parts = profileName.split("-", 2);
                    Profile profileOld = before.getOrDefault(profileName, Profile.EMPTY);
                    Profile profileNew = after.getOrDefault(profileName, Profile.EMPTY);
                    Set<String> changes = profileOld.difference(profileNew);

                    for (String change : changes) {
                        if (profileOld.contains(change)) {
                            log.debug("Deleted {} for {}", change, profileName);
                            del.setInt(1, Integer.parseInt(parts[0]));
                            del.setString(2, parts[1]);
                            del.setString(3, change);
                            del.executeUpdate();
                        } else {
                            log.debug("Created {} for {}", change, profileName);
                            add.setInt(1, Integer.parseInt(parts[0]));
                            add.setString(2, parts[1]);
                            add.setString(3, change);
                            add.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
