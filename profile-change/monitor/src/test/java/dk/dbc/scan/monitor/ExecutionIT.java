package dk.dbc.scan.monitor;

import dk.dbc.wiremock.test.WireMockFromDirectory;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.Test;

import static dk.dbc.commons.testcontainers.postgres.AbstractPgTestBase.PG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class ExecutionIT extends DB {

    @Test
    public void testJarExecution() throws Exception {
        System.out.println("testJarExecution");

        try (Connection connection = PG.createConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO profiles(agencyId, classifier, collectionIdentifier) VALUES('777777', 'xxx', '870970-basis')");
        }

        try (WireMockFromDirectory wireMock = new WireMockFromDirectory()) {
            String java = ProcessHandle.current().info().command().orElse("java");
            Process process = Runtime.getRuntime().exec(new String[] {java, "-jar", "target/datawell-scan-profile-change-monitor-jar-with-dependencies.jar",
                                                                      "-d", PG_URL,
                                                                      "-V", wireMock.url("vipcore"),
                                                                      "777777-xxx"})
                    .onExit().get();
            byte[] stdout = process.getInputStream().readAllBytes();
            System.out.write(stdout);
            System.err.write(process.getErrorStream().readAllBytes());
            int exitcode = process.exitValue();
            assertThat(exitcode, is(1));
            assertThat(new String(stdout, StandardCharsets.UTF_8), containsString("Profile 777777-xxx is changed"));
        }
    }
}
