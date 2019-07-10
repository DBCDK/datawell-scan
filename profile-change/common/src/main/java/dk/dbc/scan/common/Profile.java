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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class Profile extends HashSet<String> {

    public final static Profile EMPTY = new Profile() {
        @Override
        public boolean add(String e) {
            throw new IllegalStateException("Cannot modify an EMPTY collection");
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            throw new IllegalStateException("Cannot modify an EMPTY collection");

        }

        @Override
        public void clear() {
            throw new IllegalStateException("Cannot modify an EMPTY collection");
        }

        @Override
        public boolean remove(Object o) {
            throw new IllegalStateException("Cannot modify an EMPTY collection");

        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new IllegalStateException("Cannot modify an EMPTY collection");

        }

        @Override
        public boolean removeIf(Predicate<? super String> filter) {
            throw new IllegalStateException("Cannot modify an EMPTY collection");

        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new IllegalStateException("Cannot modify an EMPTY collection");

        }
    };

    public Profile() {
    }

    public Profile(Collection<? extends String> c) {
        super(c);
    }

    Profile(String... collectionIdenitifers) {
        super(Arrays.asList(collectionIdenitifers));
    }

    /**
     * Compute the symmetric difference
     * <p>
     * symmetric difference: union with out the intersection
     *
     * @param other the group to compare to
     * @return The group that exists in only one of the collections
     */
    public Set<String> difference(Profile other) {
        HashSet<String> diff = new HashSet<>();
        diff.addAll(missing(this, other)); // removed
        diff.addAll(missing(other, this)); // added
        return diff;
    }

    private static HashSet<String> missing(HashSet<String> before, HashSet<String> after) {
        HashSet<String> retained = new HashSet<>(before);
        retained.removeAll(after);
        return retained;
    }

    @Override
    public String toString() {
        return "Profile{" + super.toString() + '}';
    }

    /**
     * Compute the collectionIdentifiers affected by changing the profiles from
     * before to after
     *
     * @param before                How profiles were
     * @param after                 How profiles are supposed to be
     * @param collectionIdentifiers Which collectionIdentifiers were affected
     *                              (output)
     * @param holdingsAgencyIds     Which agencies had holdings that were
     *                              affected (output)
     */
    public static void allAffectedCollections(Map<String, Profile> before,
                                              Map<String, Profile> after,
                                              Set<String> collectionIdentifiers,
                                              Set<String> holdingsAgencyIds) {
        HashSet<String> allProfileNames = new HashSet<>();
        allProfileNames.addAll(before.keySet());
        allProfileNames.addAll(after.keySet());
        allProfileNames.forEach(profileName -> {
            Profile b = before.getOrDefault(profileName, EMPTY);
            Profile a = after.getOrDefault(profileName, EMPTY);
            Set<String> difference = b.difference(a);
            collectionIdentifiers.addAll(difference);
            String agencyId = profileName.split("-", 2)[0];
            // If own -katalog is included then include all holdings
            // regardless of whom own the record.
            String holdingsName = ( agencyId ) + "-katalog";
            if (difference.contains(holdingsName))
                holdingsAgencyIds.add(agencyId);
        });
    }

}
