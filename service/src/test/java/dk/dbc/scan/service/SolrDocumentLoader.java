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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SolrDocumentLoader {

    private final AtomicInteger id;
    private final SolrClient client;

    public static SolrDocumentLoader emptySolr() throws SolrServerException, IOException {
        return new SolrDocumentLoader().clear();
    }

    public SolrDocumentLoader() {
        this.id = new AtomicInteger();
        this.client = SolrApi.makeSolrClient(System.getenv("SOLR_URL"));
    }

    public SolrDocumentLoader clear() throws SolrServerException, IOException {
        client.deleteByQuery("*:*");
        return this;
    }

    public void commit() throws SolrServerException, IOException {
        client.commit(true, true);
    }

    public SolrDocumentLoader add(Consumer<Doc> content) throws SolrServerException, IOException {
        return add(1, content);
    }

    public SolrDocumentLoader add(int count, Consumer<Doc> content) throws SolrServerException, IOException {
        SolrInputDocument doc = new SolrInputDocument();
        content.accept(new Doc(doc));
        doc.setField("rec.bibliographicRecordId", id.get()); // Why did we make this mandatory?
        for (int i = 0 ; i < count ; i++) {
            doc.setField("id", "id-" + id.incrementAndGet());
            client.add(doc);
        }
        return this;
    }

    public static class Doc {

        private final SolrInputDocument doc;

        private Doc(SolrInputDocument doc) {
            this.doc = doc;
        }

        public Doc with(String field, Object value) {
            doc.addField(field, value);
            return this;
        }

        public Doc withCollectionIdentifier(Object value) {
            return with("rec.collectionIdentifier", value);
        }

        public Doc withScan(String field, Object value, String... profiles) {
            field = "scan." + field;
            doc.addField(field, value);
            field = field + "_";
            for (String profile : profiles) {
                doc.addField(field + profile, value);
            }
            return this;
        }
    }

}
