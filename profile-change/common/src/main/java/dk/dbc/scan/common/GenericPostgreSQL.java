/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-profile-change-common
 *
 * datawell-scan-profile-change-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-profile-change-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.common;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.postgresql.ds.PGSimpleDataSource;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class GenericPostgreSQL extends PGSimpleDataSource {

    private static final Pattern POSTGRES_URL_REGEX = Pattern.compile("(?:postgres(?:ql)?://)?(?:([^:@]+)(?::([^@]*))@)?([^:/]+)(?::([1-9][0-9]*))?/(.+)");

    public GenericPostgreSQL(String url) {
        Matcher matcher = POSTGRES_URL_REGEX.matcher(url);
        if (matcher.matches()) {
            String user = matcher.group(1);
            String pass = matcher.group(2);
            String host = matcher.group(3);
            String port = matcher.group(4);
            String base = matcher.group(5);
            if (user != null)
                setUser(user);
            if (pass != null)
                setPassword(pass);
            setServerNames(new String[] {host});
            if (port != null)
                setPortNumbers(new int[] {Integer.parseUnsignedInt(port)});
            setDatabaseName(base);
        } else {
            throw new IllegalArgumentException("This is not a valid database url: " + url);
        }
    }
}
