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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;


/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ProfileIT {

    @Test(timeout = 2_000L)
    public void testProfileDifference() throws Exception {
        System.out.println("testProfileDifference");

        Profile before = new Profile("a", "b", "c");
        Profile after = new Profile("b", "c", "d");

        Set<String> diff = before.difference(after);
        System.out.println("diff = " + diff);
        assertThat(diff, containsInAnyOrder("a", "d"));
    }

    @Test(timeout = 2_000L)
    public void testAllAffectedCollections() throws Exception {
        System.out.println("testAllAffectedCollections");

        Map<String, Profile> before = new HashMap<String, Profile>() {
            {
                put("123456-foo", new Profile("123456-katalog", "870970-basis"));
                put("234567-foo", new Profile("234567-katalog", "870970-basis", "999999-basic"));
                put("345678-foo", new Profile("345678-katalog", "888888-extra"));
            }
        };
        Map<String, Profile> after = new HashMap<String, Profile>() {
            {
                put("123456-foo", new Profile("870970-basis", "666666-extra"));
                put("234567-foo", new Profile("234567-katalog", "870970-basis", "999999-basic"));
                put("456789-foo", new Profile("777777-extra"));
            }
        };

        HashSet<String> collectionIdentifiers = new HashSet<>();
        HashSet<String> holdingsAgencyIds = new HashSet<>();

        Profile.allAffectedCollections(before, after, collectionIdentifiers, holdingsAgencyIds);

        System.out.println("collectionIdentifiers = " + collectionIdentifiers);
        System.out.println("holdingsAgencyIds = " + holdingsAgencyIds);

        assertThat(collectionIdentifiers, containsInAnyOrder(
                   "123456-katalog", // removed from 123456-foo
                   "666666-extra", // added to 123456-foo
                   "345678-katalog", "888888-extra", // removed from 345678-foo
                   "777777-extra" // added to 456789-foo
           ));
        assertThat(holdingsAgencyIds, containsInAnyOrder(
                "123456", // removed from 123456-foo
                "345678" // removed from 345678-foo
        ));

    }
}
