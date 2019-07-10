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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DB {

    protected String solrDocStoreUrl;
    protected PGSimpleDataSource solrDocStoreDs;
    protected String profileUrl;
    protected PGSimpleDataSource profileDs;
    protected String solrUrl;
    protected SolrClient solr;

    @Before
    public void setUpDbAndSolr() throws Exception {

        String dbBase = "localhost:" + System.getProperty("postgresql.port", "5432");
        if (dbBase.endsWith(":5432"))
            dbBase = System.getProperty("user.name") + ":" + System.getProperty("user.name") + "@" + dbBase;

        profileUrl = dbBase + "/profile";
        solrDocStoreUrl = dbBase + "/solrdocstore";
        profileDs = makeDataSource(profileUrl);
        solrDocStoreDs = makeDataSource(solrDocStoreUrl);

        try {
            solrDocStoreDs.getConnection().close();
        } catch (SQLException ex) {
            System.out.println("ex = " + ex);
            try (Connection connection = profileDs.getConnection() ;
                 Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE solrdocstore");
            }
        }
        wipeDatabase(profileDs);
        wipeDatabase(solrDocStoreDs);
        dk.dbc.scan.common.ProfileDB.migrate(profileDs);
        dk.dbc.search.solrdocstore.db.DatabaseMigrator.migrate(solrDocStoreDs, false);

        solrUrl = "zk://localhost:" + System.getProperty("solr.zk.port", "2181") + "/corepo";
        solr = SolrApi.makeSolrClient(solrUrl);

        solr.deleteByQuery("*:*");
        solr.commit();
    }

    private static void wipeDatabase(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP SCHEMA public CASCADE");
            stmt.executeUpdate("CREATE SCHEMA public");
        }
    }

    private static PGSimpleDataSource makeDataSource(String url) {
        PGSimpleDataSource ds = new PGSimpleDataSource();

        Matcher matcher = Pattern.compile("(?:postgres(?:ql)?://)?(?:([^:@]+)(?::([^@]*))@)?([^:/]+)(?::([1-9][0-9]*))?/(.+)").matcher(url);
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

    void addDocuments(Function<InputDoc, InputDoc>... func) throws SolrServerException, IOException {
        List<SolrInputDocument> docs = Arrays.stream(func)
                .map(f -> f.apply(new InputDoc()).build())
                .collect(Collectors.toList());
        solr.add(docs);
        solr.commit();
    }

    static final class InputDoc extends SolrInputDocument {

        private static final AtomicInteger recordNo = new AtomicInteger();

        InputDoc() {
            super();
        }

        InputDoc repositoryId(String id) {
            setField("rec.repositoryId", id);
            return this;
        }

        InputDoc manifestationId(String id) {

            addField("rec.manifestationId", id);
            if (getField("rec.repositoryId") == null)
                setField("rec.repositoryId", id);
            return this;
        }

        InputDoc collectionIdentifier(String id) {
            addField("rec.collectionIdentifier", id);
            return this;
        }

        InputDoc holdingsAgencyId(String id) {
            addField("rec.holdingsAgencyId", id);
            return this;
        }

        SolrInputDocument build() {
            SolrInputField field = getField("rec.manifestationId");
            if (field == null || field.getValueCount() == 0)
                throw new IllegalArgumentException("Cannot compute record-id of" + this);
            setField("id", recordNo.incrementAndGet() + "!" + field.getFirstValue());
            return this;
        }

    }

}
