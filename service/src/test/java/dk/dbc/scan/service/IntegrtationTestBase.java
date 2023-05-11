package dk.dbc.scan.service;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dk.dbc.wiremock.test.WireMockFromDirectory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.AfterClass;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class IntegrtationTestBase {

    private static final GenericContainer SOLR = makeSolr();
    public static final String SOLR_URL = UriBuilder.fromUri(makeContainerUrl(SOLR, 8983)).path("solr").path("corepo").toString();
    public static final URI ZK_URL = URI.create("zk://" + containerIp(SOLR) + ":9983/");

    public static final int WIREMOCK_PORT = getWiremockPort();
    public static final WireMockFromDirectory WIREMOCK = makeWireMock(WIREMOCK_PORT);
    public static final URI WIREMOCK_URL = getWiremockUrl(SOLR, WIREMOCK_PORT);

    @AfterClass
    public static void stopWiremock() throws Exception {
        WIREMOCK.close();
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

    public SolrDocumentLoader emptySolr() throws SolrServerException, IOException {
        return SolrDocumentLoader.emptySolr(SOLR_URL);
    }

    private static WireMockFromDirectory makeWireMock(int port) {
        return new WireMockFromDirectory("src/test/resources/wiremock",
                                         new WireMockConfiguration()
                                                 .bindAddress("0.0.0.0")
                                                 .port(port)
                                                 .stubCorsEnabled(true)
                                                 .gzipDisabled(true));
    }

    private static int getWiremockPort() {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot get wiremock port", ex);
        }
    }

    private static URI getWiremockUrl(Container container, int port) {
        return URI.create("http://" + containerNetwork(container).getGateway() + ":" + port);
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
