package dk.dbc.scan.update;

import dk.dbc.wiremock.test.WireMockFromDirectory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ExecutionIT extends DB {

    @Test
    public void testJarExecution() throws Exception {
        System.out.println("testJarExecution");

        try (Connection connection = PG.createConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM queuerule");
            stmt.execute("INSERT INTO queuerule(queue, supplier, postpone) VALUES('solr-a', 'manifestation', 0)");
            stmt.execute("INSERT INTO queuerule(queue, supplier, postpone) VALUES('solr-b', 'manifestation', 0)");
            stmt.execute("INSERT INTO profiles(agencyId, classifier, collectionIdentifier) VALUES('777777', 'xxx', '870970-basis')");
        }

        addDocuments(d -> d.collectionIdentifier("777777-katalog").manifestationId("777777-katalog:12345678").repositoryId("870970-basis:12345678"),
                     d -> d.collectionIdentifier("700000-katalog").manifestationId("700000-katalog:12345678").repositoryId("870970-basis:12345678"),
                     d -> d.collectionIdentifier("870970-basis").manifestationId("870970-basis:12345678").repositoryId("870970-basis:12345678"));

        try (WireMockFromDirectory wireMock = new WireMockFromDirectory()) {
            String java = ProcessHandle.current().info().command().orElse("java");
            Process process = Runtime.getRuntime().exec(new String[] {java, "-jar", "target/datawell-scan-profile-change-update-jar-with-dependencies.jar",
                                                                      "-d", PG_URL,
                                                                      "-s", ZK_URL,
                                                                      "-S", PG_URL,
                                                                      "-V", wireMock.url("vipcore"),
                                                                      "777777-xxx"})
                    .onExit().get();

            System.out.write(process.getInputStream().readAllBytes());
            System.err.write(process.getErrorStream().readAllBytes());
            int exitcode = process.exitValue();
            assertThat(exitcode, is(0));
        }

        HashSet<String> queue = new HashSet<>();
        try (Connection connection = PG.createConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery("SELECT consumer || '/' || jobid FROM queue")) {
            while (resultSet.next()) {
                queue.add(resultSet.getString(1));
            }
        }
        assertThat(queue, Matchers.containsInAnyOrder("solr-a-slow/777777-katalog:12345678",
                                                      "solr-b-slow/777777-katalog:12345678"));

        HashSet<String> profiles = new HashSet<>();
        try (Connection connection = PG.createConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery("SELECT agencyid || '/' || classifier || '/' || collectionIdentifier FROM profiles")) {
            while (resultSet.next()) {
                profiles.add(resultSet.getString(1));
            }
        }
        assertThat(profiles, Matchers.containsInAnyOrder("777777/xxx/777777-katalog",
                                                         "777777/xxx/870970-basis"));
    }

}
