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
package dk.dbc.scan.monitor;

import dk.dbc.scan.common.ProfileServiceActions;
import dk.dbc.scan.common.Profile;
import dk.dbc.scan.common.ProfileDB;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class Monitor {

    private final PrintStream out;

    public Monitor() {
        this.out = System.out;
    }

    Monitor(PrintStream out) {
        this.out = out;
    }

    public boolean run(Arguments arguments) throws IOException, SQLException {
        ProfileServiceActions profileService = createProfileServiceActions(arguments);
        Map<String, Profile> currentProfiles = profileService.getProfiles(arguments.getProfiles());

        ProfileDB profileDb = createProfileDb(arguments);
        Map<String, Profile> storedProfiles = profileDb.readProfiles();

        return report(storedProfiles, currentProfiles);
    }

    ProfileDB createProfileDb(Arguments arguments) {
        ProfileDB profileDB = new ProfileDB(arguments.getDb());
        profileDB.migrate();
        return profileDB;
    }

    ProfileServiceActions createProfileServiceActions(Arguments arguments) {
        return new ProfileServiceActions(arguments.getProfileService());
    }

    private boolean report(Map<String, Profile> before,
                           Map<String, Profile> after) {
        boolean same = true;
        HashSet<String> allProfileNames = new HashSet<>();
        allProfileNames.addAll(before.keySet());
        allProfileNames.addAll(after.keySet());
        for (String profileName : allProfileNames) {
            Profile b = before.get(profileName);
            Profile a = after.get(profileName);

            if (b == null) {
                same = false;
                out.println("Profile " + profileName + " is added");
            } else if (a == null) {
                same = false;
                out.println("Profile " + profileName + " is removed");
            } else {
                HashSet<String> added = new HashSet<>(a);
                added.removeAll(b);
                HashSet<String> removed = new HashSet<>(b);
                removed.removeAll(a);
                if (!added.isEmpty() || !removed.isEmpty()) {
                    same = false;
                    out.println("Profile " + profileName + " is changed");
                    if (!added.isEmpty()) {
                        out.println(" Added collectionIdentifiers:");
                        added.stream()
                                .sorted()
                                .forEach(s -> out.println("  " + s));
                    }
                    if (!removed.isEmpty()) {
                        out.println(" Removed collectionIdentifiers:");
                        removed.stream()
                                .sorted()
                                .forEach(s -> out.println("  " + s));
                    }
                }
            }
        }
        return same;
    }

}
