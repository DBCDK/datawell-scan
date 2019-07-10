/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of profile-change-monitor
 *
 * profile-change-monitor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * profile-change-monitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import dk.dbc.scan.update.Arguments;
import dk.dbc.scan.update.ExitException;
import dk.dbc.scan.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.exit;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Arguments arguments = new Arguments(args);
            log.info("monitor started");
            if (!new Update().run(arguments))
                exit(1);

        } catch (ExitException ex) {
            exit(ex.getExitCode());
        } catch (Exception ex) {
            log.error("Error: {}", ex.getMessage());
            log.debug("Error: ", ex);
            exit(1);
        } finally {
            log.info("monitor ended");
        }
        exit(0); // This is needed to stop the zookeeper threads from SolR
    }
}
