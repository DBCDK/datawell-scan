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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.DefaultStreamFactory;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Spliterator.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SolrApi {

    private static final Logger log = LoggerFactory.getLogger(SolrApi.class);

    private static final Pattern ZK = Pattern.compile("zk://([^/]*)(/.*)?/([^/]*)");

    /**
     * Make a SolrClient
     *
     * @param solrUrl http or zk url to a SolR
     * @return client
     */
    public static SolrClient makeSolrClient(String solrUrl) {
        Matcher zkMatcher = ZK.matcher(solrUrl);
        if (zkMatcher.matches()) {
            Optional<String> zkChroot = Optional.empty();
            if (zkMatcher.group(2) != null) {
                zkChroot = Optional.of(zkMatcher.group(2));
            }
            List<String> zkHosts = Arrays.asList(zkMatcher.group(1).split(","));
            CloudSolrClient solrClient = new CloudSolrClient.Builder(zkHosts, zkChroot)
                    .build();

            solrClient.setDefaultCollection(zkMatcher.group(3));

            return solrClient;
        } else {
            return new HttpSolrClient.Builder(solrUrl)
                    .build();
        }
    }

    private final String collection;
    private final SolrClientCache solrClientCache;
    private final StreamFactory streamFactory;

    public SolrApi(String solrUrl) {
        this.streamFactory = makeStreamFactory(solrUrl);
        this.solrClientCache = new SolrClientCache();
        this.collection = streamFactory.getDefaultCollection();
    }

    public Stream<ManifestationId> manifestationIdStreamOf(String query) throws IOException {
        // https://lucene.apache.org/solr/guide/8_1/stream-source-reference.html
        // https://lucene.apache.org/solr/guide/8_1/stream-decorator-reference.html
        TupleStream stream = streamFactory.constructStream(
                new StreamExpression("unique")
                        .withParameter(new StreamExpression("search")
                                .withParameter(collection)
                                .withParameter(p("qt", "/export"))
                                .withParameter(p("q", query))
                                .withParameter(p("fl", "id"))
                                .withParameter(p("sort", "id ASC"))) // Unique depends upon sorting
                        .withParameter(p("over", "id")));
        StreamContext streamContext = new StreamContext();
        streamContext.setSolrClientCache(solrClientCache);
        stream.setStreamContext(streamContext);
        return streamOf(stream)
                .map(t -> {
                    String s = t.getString("id");
                    return s.substring(s.indexOf('!') + 1); // Strip up to (including) ! which is the sharding key
                })
                .map(ManifestationId::new);
    }

    public static String queryFrom(Set<String> collectionIdentifiers, Set<String> holdingsAgencyIds) {
        return Stream.concat(
                collectionIdentifiers.stream()
                        .sorted() // For unittest matching
                        .map(ClientUtils::escapeQueryChars)
                        .map(s -> "rec.collectionIdentifier:" + s),
                holdingsAgencyIds.stream()
                        .sorted() // For unittest matching
                        .map(ClientUtils::escapeQueryChars)
                        .map(s -> "rec.holdingsAgencyId:" + s)
        ).collect(Collectors.joining(" OR "));
    }

    private static StreamFactory makeStreamFactory(String solrUrl) {
        Matcher zkMatcher = ZK.matcher(solrUrl);
        if (!zkMatcher.matches())
            throw new IllegalArgumentException("SolR url: " + solrUrl + " is not a zookeeper url");
        String hostChroot = zkMatcher.group(1);
        if (zkMatcher.group(2) != null)
            hostChroot = hostChroot + zkMatcher.group(2);
        return new DefaultStreamFactory()
                .withCollectionZkHost(
                        zkMatcher.group(3),
                        hostChroot);
    }

    private static StreamExpressionNamedParameter p(String key, String value) {
        return new StreamExpressionNamedParameter(key, value);
    }

    /**
     * Convert a TupleStream into a Java stream
     *
     * @param docs stream of docs
     * @return stream
     * @throws IOException if the stream cannot be opened
     */
    private Stream<Tuple> streamOf(TupleStream docs) throws IOException {
        docs.open();
        Spliterator<Tuple> spliterator = new Spliterator<Tuple>() {
            @Override
            public boolean tryAdvance(Consumer<? super Tuple> consumer) {
                try {
                    Tuple t = docs.read();
                    if (t.EOF) {
                        docs.close();
                        return false;
                    }
                    consumer.accept(t);
                    return true;
                } catch (IOException ex) {
                    log.error("Cannot read result from SolR: {}", ex.getMessage());
                    log.debug("Cannot read result from SolR: ", ex);
                    return false;
                }
            }

            @Override
            public Spliterator<Tuple> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return NONNULL | ORDERED;
            }
        };
        return StreamSupport.stream(spliterator, false);
    }

}
