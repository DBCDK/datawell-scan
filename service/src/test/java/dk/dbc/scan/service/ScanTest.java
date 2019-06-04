/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-service
 *
 * datawell-scan-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.service;

import javax.ejb.embeddable.EJBContainer;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ScanTest {

    @Test(timeout = 2_000L, expected = WebApplicationException.class)
    public void parameterErrorAgencyId() throws Exception {
        System.out.println("parameterErrorAgencyId");
        Scan scan = Scan.instance(null, null);
        scan.scan(null, "abc", "def", "ghi", 5, true, null);
    }

    @Test(timeout = 2_000L, expected = WebApplicationException.class)
    public void parameterErrorProfileName() throws Exception {
        System.out.println("parameterErrorProfileName");
        Scan scan = Scan.instance(null, null);
        scan.scan(123456, null, "def", "ghi", 5, true, null);
    }

    @Test(timeout = 2_000L, expected = WebApplicationException.class)
    public void parameterErrorProfileNameInvalid() throws Exception {
        System.out.println("parameterErrorProfileNameInvalid");
        Scan scan = Scan.instance(null, null);
        scan.scan(123456, "a-c", "def", "ghi", 5, true, null);
    }

    @Test(timeout = 2_000L, expected = WebApplicationException.class)
    public void parameterErrorTerm() throws Exception {
        System.out.println("parameterErrorTerm");
        Scan scan = Scan.instance(null, null);
        scan.scan(123456, "abc", null, "ghi", 5, true, null);
    }

    @Test(timeout = 2_000L, expected = WebApplicationException.class)
    public void parameterErrorRegister() throws Exception {
        System.out.println("parameterErrorRegister");
        Scan scan = Scan.instance(null, null);
        scan.scan(123456, "abc", "def", null, 5, true, null);
    }

    @Test(timeout = 2_000L, expected = WebApplicationException.class)
    public void parameterErrorCount() throws Exception {
        System.out.println("parameterErrorCount");
        Scan scan = Scan.instance(null, null);
        scan.scan(123456, "abc", "def", "ghi", 0, true, null);
    }
}
