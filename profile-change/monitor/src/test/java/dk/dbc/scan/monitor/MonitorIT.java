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

import dk.dbc.commons.testcontainers.postgres.AbstractJpaTestBase;
import dk.dbc.scan.common.Profile;
import dk.dbc.scan.common.ProfileDB;
import dk.dbc.scan.common.ProfileServiceActions;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import static dk.dbc.commons.testcontainers.postgres.AbstractJpaTestBase.PG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class MonitorIT extends DB {

    private ProfileDB profileDb;

    @Before
    public void setUp() {
        profileDb = new ProfileDB(PG_URL);
    }

    @Test(timeout = 2_000L)
    public void profileUnchanged() throws Exception {
        System.out.println("profileUnchanged");
        Profiles existingProfiles = new Profiles()
                .withProfile(
                        "102030-danbib",
                        "102030-katalog",
                        "800000-danbib",
                        "870970-danbib",
                        "870970-lokalbibl",
                        "870970-udland",
                        "870971-anmeld");
        Profiles newProfiles = new Profiles()
                .withProfile(
                        "102030-danbib",
                        "102030-katalog",
                        "800000-danbib",
                        "870970-danbib",
                        "870970-lokalbibl",
                        "870970-udland",
                        "870971-anmeld");

        ByteArrayOutputStream os = new ByteArrayOutputStream(); // Collect stdout

        storeProfile(existingProfiles);
        Monitor monitor = makeMonitor(os, newProfiles);

        Arguments a = mock(Arguments.class);
        when(a.getProfiles()).thenReturn(Collections.singletonList("102030-danbib"));
        when(a.getDb()).thenReturn(PG_URL);

        boolean same = monitor.run(a);

        String output = os.toString();
        output = Pattern.compile("\\s+", Pattern.DOTALL).matcher(output).replaceAll(" ");
        System.out.println("output = " + output);

        assertThat(same, is(true));
    }

    @Test(timeout = 2_000L)
    public void profileChanged() throws Exception {
        System.out.println("profileChanged");
        Profiles existingProfiles = new Profiles()
                .withProfile(
                        "102030-danbib",
                        "102030-katalog",
                        //"800000-danbib",
                        "870970-danbib",
                        "870970-lokalbibl",
                        "870970-udland",
                        "870971-anmeld");
        Profiles newProfiles = new Profiles()
                .withProfile(
                        "102030-danbib",
                        //"102030-katalog",
                        "800000-danbib",
                        "870970-danbib",
                        "870970-lokalbibl",
                        "870970-udland",
                        "870971-anmeld");

        ByteArrayOutputStream os = new ByteArrayOutputStream(); // Collect stdout

        storeProfile(existingProfiles);
        Monitor monitor = makeMonitor(os, newProfiles);

        Arguments a = mock(Arguments.class);
        when(a.getProfiles()).thenReturn(Collections.singletonList("102030-danbib"));
        when(a.getDb()).thenReturn(PG_URL);

        boolean same = monitor.run(a);

        String output = os.toString();
        output = Pattern.compile("\\s+", Pattern.DOTALL).matcher(output).replaceAll(" ");
        System.out.println("output = " + output);

        assertThat(same, is(false));
        assertThat(output, containsString("Profile 102030-danbib is changed"));
        assertThat(output, containsString("Removed collectionIdentifiers: 102030-katalog"));
        assertThat(output, containsString("Added collectionIdentifiers: 800000-danbib"));
    }

    @Test(timeout = 2_000L)
    public void addedProfile() throws Exception {
        System.out.println("addedProfile");

        Profiles existingProfiles = new Profiles()
                .withProfile(
                        "102030-danbib",
                        "102030-katalog",
                        "800000-danbib",
                        "870970-danbib",
                        "870970-lokalbibl",
                        "870970-udland",
                        "870971-anmeld");
        Profiles newProfiles = new Profiles()
                .withProfile(
                        "102030-danbib",
                        "102030-katalog",
                        "800000-danbib",
                        "870970-danbib",
                        "870970-lokalbibl",
                        "870970-udland",
                        "870971-anmeld")
                .withProfile(
                        "102030-extra",
                        "870970-danbib");

        ByteArrayOutputStream os = new ByteArrayOutputStream(); // Collect stdout

        storeProfile(existingProfiles);
        Monitor monitor = makeMonitor(os, newProfiles);

        Arguments a = mock(Arguments.class);
        when(a.getProfiles()).thenReturn(Arrays.asList("102030-danbib", "102030-extra"));
        when(a.getDb()).thenReturn(PG_URL);

        boolean same = monitor.run(a);

        String output = os.toString();
        output = Pattern.compile("\\s+", Pattern.DOTALL).matcher(output).replaceAll(" ");
        System.out.println("output = " + output);

        assertThat(same, is(false));
        assertThat(output, containsString("Profile 102030-extra is added"));
    }

    @Test(timeout = 5_000L)
    public void removedProfile() throws Exception {
        System.out.println("removedProfile");

        Profiles existingProfiles = new Profiles()
                .withProfile(
                        "102030-danbib",
                        "102030-katalog",
                        "800000-danbib",
                        "870970-danbib",
                        "870970-lokalbibl",
                        "870970-udland",
                        "870971-anmeld")
                .withProfile(
                        "102030-extra",
                        "870970-danbib");
        Profiles newProfiles = new Profiles()
                .withProfile(
                        "102030-danbib",
                        "102030-katalog",
                        "800000-danbib",
                        "870970-danbib",
                        "870970-lokalbibl",
                        "870970-udland",
                        "870971-anmeld");

        ByteArrayOutputStream os = new ByteArrayOutputStream(); // Collect stdout

        storeProfile(existingProfiles);
        Monitor monitor = makeMonitor(os, newProfiles);

        Arguments a = mock(Arguments.class);
        when(a.getProfiles()).thenReturn(Arrays.asList("102030-danbib"));
        when(a.getDb()).thenReturn(PG_URL);

        boolean same = monitor.run(a);

        String output = os.toString();
        output = Pattern.compile("\\s+", Pattern.DOTALL).matcher(output).replaceAll(" ");
        System.out.println("output = " + output);

        assertThat(same, is(false));
        assertThat(output, containsString("Profile 102030-extra is removed"));
    }

    private void storeProfile(HashMap<String, Profile> profiles) throws SQLException {
        profileDb.updateProfiles(profileDb.readProfiles(), profiles);
    }

    private Monitor makeMonitor(OutputStream os, Profiles newProfiles) throws UnsupportedEncodingException {
        return new Monitor(new PrintStream(os, true, "utf-8")) {
            @Override
            ProfileServiceActions createProfileServiceActions(Arguments arguments) {
                try {
                    ProfileServiceActions m = mock(ProfileServiceActions.class);
                    when(m.getProfiles(anyList())).thenCallRealMethod();
                    when(m.getProfile(anyString()))
                            .then(i -> {
                                String profileName = i.getArgument(0);
                                return newProfiles.get(profileName);
                            });
                    return m;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    private static Profile makeProfile(String... collectionIdetifiers) {
        Profile profile = new Profile();
        profile.addAll(Arrays.asList(collectionIdetifiers));
        return profile;
    }

    private static class Profiles extends HashMap<String, Profile> {

        public Profiles withProfile(String profileName, String... collectionIdetifiers) {
            put(profileName, makeProfile(collectionIdetifiers));
            return this;
        }
    }
}
