/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-profile-change-common
 *
 * datawell-scan-profile-change-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-profile-change-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.common;

import java.io.IOException;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ReThrowExceptionTest {

    @Test(timeout = 2_000L)
    public void testSimpleRethrow() {
        System.out.println("testSimpleRethrow");
        try {
            try {
                ReThrowException.wrap(() -> {
                    throw new IOException(new IllegalStateException("arg"));
                });
                fail("Sould have thrown an exception");
            } catch (ReThrowException e) {
                String message = e.getMessage();
                assertThat(message, is(IllegalStateException.class.getCanonicalName() + ": arg"));
                e.throwAs(IllegalStateException.class);
                e.throwAs(Exception.class);
            } catch (Exception e) {
                fail("Should have thrown a ReThrowException");
            }
        } catch (IllegalStateException e) {
        } catch (Exception e) {
            fail();
        }
    }
}
