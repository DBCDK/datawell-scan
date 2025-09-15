/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of scan-service
 *
 * scan-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * scan-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.service;

import dk.dbc.commons.payara.helpers.MDCRequestInfo;
import java.util.Set;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@ApplicationPath("api")
public class ScanServiceApplication extends Application {

    private static final Set<Class<?>> CLASSES = Set.of(
            Scan.class,
            StatusBean.class,
            MDCRequestInfo.class
    );

    @Override
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }
}
