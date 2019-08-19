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

import java.util.Map;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ProfileServiceActionsIT {

    @Test(timeout = 2_000L)
    public void testGetProfile() throws Exception {
        System.out.println("testGetProfile");

        ProfileServiceActions psa = new ProfileServiceActions("http://localhost:" + System.getProperty("wiremock.port", "80") + "/profile-service/");

        Profile profile = psa.getProfile("102030-danbib");
        assertThat(profile.contains("800000-danbib"), is(true));
        assertThat(profile.contains("870970-danbib"), is(true));
    }

    @Test(timeout = 2_000L)
    public void testGetProfiles() throws Exception {
        System.out.println("testGetProfiles");

        ProfileServiceActions psa = new ProfileServiceActions("http://localhost:" + System.getProperty("wiremock.port", "80") + "/profile-service/");

        Map<String, Profile> profiles = psa.getProfiles(asList("102030-danbib"));
        assertThat(profiles.size(), is(1));
        assertThat(profiles.get("102030-danbib"), notNullValue());
        Profile profile = profiles.get("102030-danbib");
        assertThat(profile.contains("800000-danbib"), is(true));
        assertThat(profile.contains("870970-danbib"), is(true));
    }
}