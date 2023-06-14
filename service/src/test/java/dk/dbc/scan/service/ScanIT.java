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

import dk.dbc.scan.service.ScanResponse.Term;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ScanIT extends IntegrationTestBase {

    @Test(timeout = 20_000L)
    public void testCase() throws Exception {
        System.out.println("testCase");

        emptySolr()
                .add(4, d -> d
                     .withScan("lti", "hello there", "123456_that")
                     .withCollectionIdentifier("777777-katalog")
                     .withCollectionIdentifier("654321-danbib"))
                .add(1, d -> d
                     .withScan("lti", "hello abe", "123456_that")
                     .withCollectionIdentifier("777777-katalog")
                     .withCollectionIdentifier("654321-danbib"))
                .add(1, d -> d
                     .withScan("lti", "hello butler") // Not part of 123456 profile
                     .withCollectionIdentifier("777777-katalog"))
                .add(1, d -> d
                     .withScan("lti", "hello cat", "123456_that")
                     .withCollectionIdentifier("777777-katalog")
                     .withCollectionIdentifier("654321-danbib"))
                .add(3, d -> d
                     .withScan("lti", "hello dolly", "123456_that")
                     .withCollectionIdentifier("777777-katalog")
                     .withCollectionIdentifier("654321-danbib"))
                .add(1, d -> d
                     .withScan("lti", "hello ecma", "123456_that")
                     .withCollectionIdentifier("777777-katalog")
                     .withCollectionIdentifier("654321-danbib"))
                .add(4, d -> d
                     .withScan("lti", "hello world", "123456_that")
                     .withCollectionIdentifier("777777-katalog")
                     .withCollectionIdentifier("654321-danbib"))
                .add(2, d -> d
                     .withScan("lti", "zoo", "123456_that")
                     .withCollectionIdentifier("777777-katalog")
                     .withCollectionIdentifier("654321-danbib"))
                .add(2, d -> d
                        .withScan("lti", "", "123456_that") // empty value
                        .withCollectionIdentifier("777777-katalog")
                        .withCollectionIdentifier("654321-danbib"))
                .add(2, d -> d
                        .withScan("lti", null, "123456_that") // null value
                        .withCollectionIdentifier("777777-katalog")
                        .withCollectionIdentifier("654321-danbib"))
                .commit();

        ExecutorService mes = Executors.newFixedThreadPool(25);

        Config config = new Config(
                "SOLR_URL=" + ZK_URL + "corepo",
                "SOLR_APPID=datawellscan",
                "VIPCORE_ENDPOINT=" + WIREMOCK_URL + "/vipcore/api"
        );
        config.init();

        ProfileServiceCache psCache = ProfileServiceCache.instance(config);
        SolrApi solrApi = SolrApi.instance(config);
        ScanLogic scanLogic = ScanLogic.instance(config, psCache, solrApi, mes);
        Scan scan = Scan.instance(config, scanLogic);

        Set<String> terms1 = scan.scan(123456, "that", "hello", "scan.lti", 20, false, "test")
                .getResult()
                .getTerms()
                .stream()
                .map(Term::getTerm)
                .collect(toSet());
        assertThat(terms1, hasItem("hello world"));
        assertThat(terms1, hasItem("zoo"));
        assertThat(terms1, not(hasItem("hello butler")));

        assertThat(terms1.size(), is(7)); // test that null and empty terms no not get included in response

        Set<String> terms2 = scan.scan(123456, "that", "hello", "scan.lti", 2, false, "test")
                .getResult()
                .getTerms()
                .stream()
                .map(Term::getTerm)
                .collect(toSet());
        assertThat(terms2, hasItems("hello abe", "hello cat"));
        assertThat(terms2.size(), is(2));
        assertThat(terms2, not(hasItem("hello world")));


        List<Runnable> pending = mes.shutdownNow();
        assertThat(pending, is(Collections.EMPTY_LIST));
    }
}
