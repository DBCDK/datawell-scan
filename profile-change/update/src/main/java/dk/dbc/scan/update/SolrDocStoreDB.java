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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SolrDocStoreDB {

    private static final Pattern POSTGRES_URL_REGEX = Pattern.compile("(?:postgres(?:ql)?://)?(?:([^:@]+)(?::([^@]*))@)?([^:/]+)(?::([1-9][0-9]*))?/(.+)");

    private final DataSource ds;

    public SolrDocStoreDB(String url) {
        this.ds = makeDataSource(url);
    }

    public List<String> getQueues() throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        try (Connection connection = ds.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT queue FROM queuerule")) {
            while (resultSet.next()) {
                list.add(resultSet.getString(1) + "-slow");
            }
        }
        return list;
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    /**
     * Construct a datasource
     *
     * @param url postgresql url
     * @return datasource
     */
    static DataSource makeDataSource(String url) {
        PGSimpleDataSource ds = new PGSimpleDataSource();

        Matcher matcher = POSTGRES_URL_REGEX.matcher(url);
        if (matcher.matches()) {
            String user = matcher.group(1);
            String pass = matcher.group(2);
            String host = matcher.group(3);
            String port = matcher.group(4);
            String base = matcher.group(5);
            if (user != null)
                ds.setUser(user);
            if (pass != null)
                ds.setPassword(pass);
            ds.setServerName(host);
            if (port != null)
                ds.setPortNumber(Integer.parseUnsignedInt(port));
            ds.setDatabaseName(base);
            return ds;
        } else {
            throw new IllegalArgumentException("This is not a valid database url: " + url);
        }
    }

}
