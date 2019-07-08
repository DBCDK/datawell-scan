/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-profile-change-update
 *
 * datawell-scan-profile-change-update is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-profile-change-update is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.update;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ArgumentsTest {

    @Test(timeout = 2_000L)
    public void testHappyPath() throws Exception {
        System.out.println("testHappyPath");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        Arguments arguments = new Arguments("-d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +")) {
            @Override
            OutputStream getOutputStream(boolean hasError) {
                return bos;
            }
        };
        String err = new String(bos.toByteArray(), UTF_8);
        assertThat(err, is(""));
        assertThat(arguments.getProfileDb(), is("myDb"));
        assertThat(arguments.getSolrDocStoreDb(), is("otherDb"));
        assertThat(arguments.getSolrUrl(), is("http://localhost/solr"));
        assertThat(arguments.getProfileService(), is("http://localhost/profile-service"));
        assertThat(arguments.getBatchSize(), is(10000));
        assertThat(arguments.getProfiles(), containsInAnyOrder("700000-bar"));
    }

    @Test(timeout = 2_000L)
    public void testMissingProfileDb() throws Exception {
        System.out.println("testMissingProfileDb");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +")) {
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
    public void testMissingSolrDb() throws Exception {
        System.out.println("testMissingSolrDb");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("Missing required options: S"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testMissingSolrUrl() throws Exception {
        System.out.println("testMissingSolrUrl");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -S otherDb -p http://localhost/profile-service 700000-bar".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("Missing required options: s"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testMissingProfileUrl() throws Exception {
        System.out.println("testMissingProfileUrl");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -S otherDb -s http://localhost/solr 700000-bar".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("Missing required options: p"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testMissingProfile() throws Exception {
        System.out.println("testMissingProfile");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service".split(" +")) {
                @Override
                OutputStream getOutputStream(boolean hasError) {
                    return bos;
                }
            };
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("Missing required options: PROFILE"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testProfileSyntax1() throws Exception {
        System.out.println("testProfileSyntax1");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service xxxxxx-yyy".split(" +")) {
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
    public void testProfileSyntax2() throws Exception {
        System.out.println("testProfileSyntax2");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new Arguments("-d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service xxxxxx-".split(" +")) {
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
            new Arguments("-d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service -v -q 700000-bar".split(" +")) {
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

    @Test(timeout = 2_000L)
    public void testBatchValue() throws Exception {
        System.out.println("testBatchValue");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Arguments arguments = new Arguments("-b 12345 -d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +")) {
            @Override
            OutputStream getOutputStream(boolean hasError) {
                return bos;
            }
        };
        String err = new String(bos.toByteArray(), UTF_8);
        assertThat(err, is(""));
        assertThat(arguments.getBatchSize(), is(12345));
    }

    @Test(timeout = 2_000L)
    public void testBatchDefault() throws Exception {
        System.out.println("testBatchDefault");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Arguments arguments = new Arguments("-d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +")) {
            @Override
            OutputStream getOutputStream(boolean hasError) {
                return bos;
            }
        };
        String err = new String(bos.toByteArray(), UTF_8);
        assertThat(err, is(""));
        assertThat(arguments.getBatchSize(), is(10000));
    }

    @Test(timeout = 2_000L)
    public void testHasQueues() throws Exception {
        System.out.println("testHasQueues");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Arguments arguments = new Arguments("-d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +")) {
            @Override
            OutputStream getOutputStream(boolean hasError) {
                return bos;
            }
        };
        String err = new String(bos.toByteArray(), UTF_8);
        assertThat(err, is(""));
        assertThat(arguments.hasQueue(), is(false));
    }

    @Test(timeout = 2_000L)
    public void testQueuesList() throws Exception {
        System.out.println("testQueuesList");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Arguments arguments = new Arguments("-Q abc,,12 -d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +")) {
            @Override
            OutputStream getOutputStream(boolean hasError) {
                return bos;
            }
        };
        String err = new String(bos.toByteArray(), UTF_8);
        assertThat(err, is(""));
        assertThat(arguments.hasQueue(), is(true));
        assertThat(arguments.getQueues(), containsInAnyOrder("abc", "12"));
    }

    @Test(timeout = 2_000L)
    public void testBadQueueList() throws Exception {
        System.out.println("testBadQueueList");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Arguments arguments = new Arguments("-Q ,, -d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +")) {
            @Override
            OutputStream getOutputStream(boolean hasError) {
                return bos;
            }
        };
        try {
            arguments.getQueues();
        } catch (ExitException e) {
            assertThat(e.getExitCode(), not(is(0)));
            String err = new String(bos.toByteArray(), UTF_8);
            assertThat(err, containsString("no queues defined"));
            return;
        }
        fail("Did not throw ExitException");
    }

    @Test(timeout = 2_000L)
    public void testQuiet() throws Exception {
        System.out.println("testQuiet");

        new Arguments("-q -d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +"));
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertThat(context.getLogger("dk.dbc").getLevel(), is(Level.WARN));
    }

    @Test(timeout = 2_000L)
    public void testVerbose() throws Exception {
        System.out.println("testVerbose");

        new Arguments("-v -d myDb -S otherDb -s http://localhost/solr -p http://localhost/profile-service 700000-bar".split(" +"));
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertThat(context.getLogger("dk.dbc").getLevel(), is(Level.DEBUG));
    }
}
