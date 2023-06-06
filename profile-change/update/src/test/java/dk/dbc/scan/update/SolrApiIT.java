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

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;


/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SolrApiIT extends DB {

    @Test(timeout = 120_000L)
    public void streamManifestationIds() throws Exception {
        System.out.println("streamManifestationIds");

        addDocuments(
                d -> d.manifestationId("870970-basis:12345678")
                        .collectionIdentifier("870970-basis")
                        .collectionIdentifier("800000-danbib"),
                d -> d.manifestationId("777777-katalog:12345678")
                        .repositoryId("870970-basis:12345678")
                        .collectionIdentifier("777777-katalog")
        );

        ArrayList<String> manifestationIds = new ArrayList<>();
        SolrApi solrApi = new SolrApi(ZK_URL + "corepo");
        solrApi.manifestationIdStreamOf("*:*")
                .map(Object::toString)
                .forEach(manifestationIds::add);

        System.out.println("manifestationIds = " + manifestationIds);

        assertThat(manifestationIds, containsInAnyOrder(
                   "ManifestationId{870970-basis:12345678}",
                   "ManifestationId{777777-katalog:12345678}"
           ));
    }
}
