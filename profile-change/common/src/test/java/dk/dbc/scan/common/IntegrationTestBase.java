package dk.dbc.scan.common;

import dk.dbc.commons.testcontainers.postgres.AbstractJpaTestBase;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import java.util.Collection;
import java.util.Collections;
import javax.sql.DataSource;
import org.testcontainers.containers.GenericContainer;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class IntegrationTestBase extends AbstractJpaTestBase {

    public static final String PG_URL = pgUrl(PG);

    @Override
    public void migrate(DataSource ds) {
        ProfileDB.migrate(ds);
    }

    @Override
    public String persistenceUnitName() {
        return null;
    }

    @Override
    public Collection<String> keepContentOfTables() {
        return Collections.singleton("flyway_schema_history");
    }

    private static String pgUrl(DBCPostgreSQLContainer container) {
        return container.getUsername() + ":" + container.getPassword() + "@" + containerIp(container) + ":5432/" + container.getDatabaseName();
    }

    private static String containerIp(GenericContainer container) {
        return container.getContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No docker network available"))
                .getIpAddress();
    }
}
