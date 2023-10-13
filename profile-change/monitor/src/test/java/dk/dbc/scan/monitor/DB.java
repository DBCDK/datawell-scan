package dk.dbc.scan.monitor;

import dk.dbc.commons.testcontainers.postgres.AbstractJpaTestBase;
import dk.dbc.scan.common.ProfileDB;
import java.util.Collection;
import java.util.Collections;
import javax.sql.DataSource;

import static dk.dbc.commons.testcontainers.postgres.AbstractPgTestBase.PG;

public class DB extends AbstractJpaTestBase {

    public static final String PG_URL = pgUrl();

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

    private static String pgUrl() {
        String ip = PG.getContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No docker network available"))
                .getIpAddress();
        return PG.getUsername() + ":" + PG.getPassword() + "@" + ip + ":5432/" + PG.getDatabaseName();
    }

}
