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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;

import static dk.dbc.scan.service.SolrDocumentLoader.emptySolr;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ScanIT {

    @Test(timeout = 20_000L)
    public void testCase() throws Exception {
        System.out.println("testCase");

        emptySolr()
                .add(4, d -> d
                     .withScan("lti", "hello there", "123456_that")
                     .withCollectionIdentifier("777777-foo")
                     .withCollectionIdentifier("654321-bar"))
                .add(1, d -> d
                     .withScan("lti", "hello abe", "123456_that")
                     .withCollectionIdentifier("777777-foo")
                     .withCollectionIdentifier("654321-bar"))
                .add(1, d -> d
                     .withScan("lti", "hello butler") // Not part of 123456 profile
                     .withCollectionIdentifier("777777-foo"))
                .add(1, d -> d
                     .withScan("lti", "hello cat", "123456_that")
                     .withCollectionIdentifier("777777-foo")
                     .withCollectionIdentifier("654321-bar"))
                .add(3, d -> d
                     .withScan("lti", "hello dolly", "123456_that")
                     .withCollectionIdentifier("777777-foo")
                     .withCollectionIdentifier("654321-bar"))
                .add(1, d -> d
                     .withScan("lti", "hello ecma", "123456_that")
                     .withCollectionIdentifier("777777-foo")
                     .withCollectionIdentifier("654321-bar"))
                .add(4, d -> d
                     .withScan("lti", "hello world", "123456_that")
                     .withCollectionIdentifier("777777-foo")
                     .withCollectionIdentifier("654321-bar"))
                .add(2, d -> d
                     .withScan("lti", "zoo", "123456_that")
                     .withCollectionIdentifier("777777-foo")
                     .withCollectionIdentifier("654321-bar"))
                .commit();

        ExecutorService mes = Executors.newFixedThreadPool(25);

        Config config = Config.instance();
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
