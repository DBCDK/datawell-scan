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

import dk.dbc.scan.common.GenericPostgreSQL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SolrDocStoreDB extends GenericPostgreSQL {


    public SolrDocStoreDB(String url) {
        super(url);
    }

    public List<String> getQueues() throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        try (Connection connection = getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT queue FROM queuerule")) {
            while (resultSet.next()) {
                list.add(resultSet.getString(1) + "-slow");
            }
        }
        return list;
    }
}
