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
package dk.dbc.scan.monitor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ArgumentsTest {

    @Test(timeout = 2_000L)
    public void testHappyPath() throws Exception {
        System.out.println("testHappyPath");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Arguments arguments = new Arguments("-d myDb -V http://localhost/goo 700000-bar".split(" +")) {
            @Override
                    OutputStream getOutputStream(boolean hasError) {
                        return bos;
                    }
        };
        String err = new String(bos.toByteArray(), UTF_8);
        assertThat(err, is(""));
        assertThat(arguments.getDb(), is("myDb"));
        assertThat(arguments.getVipCore(), is("http://localhost/goo"));
        assertThat(arguments.getProfiles(), containsInAnyOrder("700000-bar"));
    }

    @Test(timeout = 2_000L)
    public void testMissingDb() throws Exception {
        System.out.println("testMissingDb");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-V http://localhost/goo 700000-bar".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("Missing required options: d"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testMissingVipCore() throws Exception {
        System.out.println("testMissingVipCore");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb 700000-bar".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("Missing required options: V"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testMissingProfiles() throws Exception {
        System.out.println("testMissingProfiles");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -V http://localhost/goo ".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("PROFILE"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testProfileSyntaxError1() throws Exception {
        System.out.println("testProfileSyntaxError1");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -V http://localhost/goo xxxxxx-yyy".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("profiles are in the format"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testProfileSyntaxError2() throws Exception {
        System.out.println("testProfileSyntaxError2");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -V http://localhost/goo 700000-".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("profiles are in the format"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testVandQ() throws Exception {
        System.out.println("testVandQ");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -V http://localhost/goo -v -q 700000-bar".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("both -q and -v"));
            return;
        }
        fail("Did not throw ExitException");
    }
}
