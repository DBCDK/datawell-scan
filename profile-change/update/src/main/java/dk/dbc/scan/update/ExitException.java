/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-profile-change-monitor
 *
 * datawell-scan-profile-change-monitor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-profile-change-monitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.update;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ExitException extends RuntimeException {

    private static final long serialVersionUID = -4270604669315795105L;
    private final int exitCode;

    public ExitException(int exitCode) {
        this.exitCode = exitCode;
    }

    public ExitException(int exitCode, String string) {
        super(string);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

}
