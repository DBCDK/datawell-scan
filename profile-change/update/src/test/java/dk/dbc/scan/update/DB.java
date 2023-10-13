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

import com.github.dockerjava.api.model.ContainerNetwork;
import dk.dbc.commons.testcontainers.postgres.AbstractJpaTestBase;
import dk.dbc.scan.common.ProfileDB;
import dk.dbc.search.solrdocstore.db.DatabaseMigrator;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.Before;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import static dk.dbc.commons.testcontainers.postgres.AbstractJpaTestBase.PG;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DB extends AbstractJpaTestBase {

    public static final String PG_URL = pgUrl();
    private SolrClient solr;

    private static final GenericContainer SOLR = makeSolr();
    public static final String SOLR_URL = UriBuilder.fromUri(makeContainerUrl(SOLR, 8983)).path("solr").path("corepo").toString();
    public static final String ZK_URL = "zk://" + containerIp(SOLR) + ":9983/corepo";

    @Override
    public void migrate(DataSource ds) {
        DatabaseMigrator.migrate(ds);
        ProfileDB.migrate(ds);
    }

    @Override
    public String persistenceUnitName() {
        return null;
    }

    @Override
    public Collection<String> keepContentOfTables() {
        return List.of("flyway_schema_history", "queuesuppliers", "schema_version", "solr_doc_store_queue_version", "queue_version");
    }

    private static String pgUrl() {
        String ip = containerNetwork(PG).getIpAddress();
        return PG.getUsername() + ":" + PG.getPassword() + "@" + ip + ":5432/" + PG.getDatabaseName();
    }

    @Before
    public void setUpSolr() throws Exception {
        solr = SolrApi.makeSolrClient(ZK_URL);
        solr.deleteByQuery("*:*");
        solr.commit();
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

        SolrInputDocument build() {
            SolrInputField field = getField("rec.manifestationId");
            if (field == null || field.getValueCount() == 0)
                throw new IllegalArgumentException("Cannot compute record-id of" + this);
            setField("id", recordNo.incrementAndGet() + "!" + field.getFirstValue());
            return this;
        }

    }

    private static GenericContainer makeSolr() {
        String fromImage = "docker-dbc.artifacts.dbccloud.dk/dbc-solr9:latest";
        dockerPull(fromImage);

        ImageFromDockerfile image = new ImageFromDockerfile()
                .withFileFromPath("target/solr/corepo-config", Path.of("target/solr/corepo-config").toAbsolutePath())
                .withDockerfileFromBuilder(dockerfile ->
                        dockerfile.from(fromImage)
                                .add("target/solr/corepo-config/", "/collections/corepo/")
                                .user("root")
                                .run("chown -R $SOLR_USER:$SOLR_USER /opt/solr/server/solr")
                                .user("$SOLR_USER")
                                .build());
        GenericContainer solr = new GenericContainer(image)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dk.dbc.SOLR")))
                .withEnv("ZKSTRING", "localhost")
                .withExposedPorts(8983)
                .waitingFor(Wait.forHttp("/solr/corepo/select?q=*:*"))
                .withStartupTimeout(Duration.ofMinutes(1));
        solr.start();
        return solr;
    }

    private static URI makeContainerUrl(GenericContainer container, int port) {
        return URI.create("http://" + containerIp(container) + ":" + port);
    }

    private static String containerIp(GenericContainer container) {
        return containerNetwork(container).getIpAddress();
    }

    private static ContainerNetwork containerNetwork(Container container) {
        return container.getContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No docker network available"));
    }

    private static void dockerPull(String image) {
        try {
            // pull image
            DockerImageName from = DockerImageName.parse(image);
            DockerClientFactory.instance().client()
                    .pullImageCmd(from.getUnversionedPart())
                    .withTag(from.getVersionPart())
                    .start()
                    .awaitCompletion(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

}
