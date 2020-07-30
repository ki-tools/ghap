package io.ghap.logevents;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.*;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.Refresh;
import io.searchbox.indices.mapping.DeleteMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import vc.inreach.aws.request.AWSSigner;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import com.google.common.base.Supplier;

import static java.util.Objects.requireNonNull;

public class ElasticSearchClient implements EventsClient {

    private static final Log log = LogFactory.getLog(ElasticSearchClient.class);

    private final JestClient jestClient;
    private final Indexes index;

    public ElasticSearchClient(Indexes index, Map config) {
        this.index = Objects.requireNonNull(index, "Index should be specified");
        String endpointUri = getEndpointUri(config);
        log.info("ElasticSearch uri is \"" + endpointUri + "\"");

        // Date
        Supplier<LocalDateTime> clock = () -> LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
        Region region = Regions.getCurrentRegion();
        String regionName = (region == null) ? "us-east-1":region.getName();
        /*
         * AWS credentials provider chain that looks for credentials in this order:
         * Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
         * Java System Properties - aws.accessKeyId and aws.secretKey
         * Instance profile credentials delivered through the Amazon EC2 metadata service
         */
        AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AWSSigner awsSigner = new AWSSigner(awsCredentialsProvider, regionName, "es", clock);
        final AWSSigningRequestInterceptor requestInterceptor = new AWSSigningRequestInterceptor(awsSigner);
        final JestClientFactory factory = new JestClientFactory() {
            @Override
            protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
                // See DefaultHttpRequestRetryHandler.requestSentRetryEnabled
                //
                // true if it's OK to retry non-idempotent requests that have been sent
                // and then fail with network issues (not HTTP failures).
                //
                // "true" here will retry POST requests which have been sent but where
                // the response was not received. This arguably is a bit risky.
                //
                // Retries are logged at INFO level to org.apache.http.impl.execchain.RetryExec
                boolean requestSentRetryEnabled = true;
                builder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, requestSentRetryEnabled));

                builder.addInterceptorLast(requestInterceptor);
                return builder;
            }
            @Override
            protected HttpAsyncClientBuilder configureHttpClient(HttpAsyncClientBuilder builder) {
                builder.addInterceptorLast(requestInterceptor);
                return builder;
            }
        };

        HttpClientConfig clientConfig = new HttpClientConfig.Builder(
                endpointUri).connTimeout(60000).readTimeout(60000).build();

        factory.setHttpClientConfig(clientConfig);
        jestClient = factory.getObject();
    }

    @Override
    public void send(Types type, Map data) throws IOException {
        DocumentResult result = withMandatoryFields(data).execute(new Index.Builder(data).index(index.toIndex()).type(type.toType()).build());
        if( !result.isSucceeded() ){
            throw new IOException("Error during send to " + error(type, result));
        }
    }

    @Override
    public void clearAll(Types type) throws IOException {
        JestResult result = jestClient.execute(new DeleteMapping.Builder(index.toIndex(), type.toType()).refresh(true).build());
        if( !result.isSucceeded() ){
            throw new IOException("Error during clear " + error(type, result));
        }
    }

    @Override
    public void clearAll() throws IOException {
        boolean indexExists = jestClient.execute(new IndicesExists.Builder(index.toIndex()).build()).isSucceeded();
        if (indexExists) {
            jestClient.execute(new DeleteIndex.Builder(index.toIndex()).build());
        }
        jestClient.execute(new CreateIndex.Builder(index.toIndex()).refresh(true).build());
    }

    @Override
    public void sendAsync(Types type, Map data, ResultHandler<JsonObject> resultHandler) throws IOException {
        withMandatoryFields(data).executeAsync(new Index.Builder(data).index(index.toIndex()).type(type.toType()).build(), new JestResultHandler<DocumentResult>() {

            @Override
            public void completed(DocumentResult result) {
                if (!result.isSucceeded()) {
                    resultHandler.failed(new IOException("Error during send to " + error(type, result)));
                } else {
                    resultHandler.completed(result.getJsonObject());
                }
            }

            @Override
            public void failed(Exception ex) {
                resultHandler.failed(ex);
            }
        });
    }

    @Override
    public void refresh() throws IOException {
        JestResult result = jestClient.execute(new Refresh.Builder().build());
        if( !result.isSucceeded() ){
            throw new IOException("Error during clear " + error(null, result));
        }
    }

    @Override
    public List<SearchResult.Hit<EventItem, Void>> read(Types type, Date start, Date end) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(Integer.MAX_VALUE);

        if (start != null || end != null) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("timestamp");
            if (start != null) {
                rangeQueryBuilder.gte(start);
            }
            if (end != null) {
                rangeQueryBuilder.lte(end);
            }
            searchSourceBuilder.query(rangeQueryBuilder);
        }
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(index.toIndex())
                .addType(type.toType())
                .addSort(new Sort("timestamp"))
                .build();
        SearchResult result = jestClient.execute(search);

        if( !result.isSucceeded() ){
            throw new IOException("Error during read from " + error(type, result));
        }
        List<SearchResult.Hit<EventItem, Void>> hits = result.getHits(EventItem.class);
        return hits;
    }

    @Override
    public void close() {
        jestClient.shutdownClient();
    }

    private static String getEndpointUri(Map config){
        String endpointUri = requireNonNull((String) config.get("uri"), "Elasticsearch \"uri\" is not provided");
        endpointUri = endpointUri.replace('\\','/');
        if(endpointUri.endsWith("/")){
            endpointUri = endpointUri.substring(0, endpointUri.length() - 1);
        }
        return endpointUri;
    }

    private JestClient withMandatoryFields(Map data) throws UnknownHostException {
        if( !data.containsKey("clientip") ) {
            try {
                data.put("clientip", InetUtils.getLocalIP(false));
            } catch (SocketException e) {
                data.put("clientip", "undefined");
            }
        }
        data.put("timestamp", new Date());
        return jestClient;
    }

    private String error(Types type, JestResult result){
        String typeStr = (type == null) ? "":("." + type.toType());
        return "\"" + index.toIndex() + typeStr + "\". " + result.getErrorMessage() + ". Response: " + result.getJsonString();
    }

}
