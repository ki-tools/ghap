package io.ghap.logevents;

import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.util.IOUtils.closeQuietly;
import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ElasticSearchClientTest {

    private static EventsClient eventsClient;
    private static final Log log = LogFactory.getLog(ElasticSearchClientTest.class);

    @BeforeClass
    public static void setUpEventsClient() throws IOException {
        eventsClient = createClient();
        eventsClient.clearAll();
    }

    @AfterClass
    public static void tearDownEventsClient() {
        try {
            eventsClient.close();
        } catch (Exception e) {
            closeQuietly(eventsClient, log);
        }
    }

    /**
     * Test case given in AWS Signing Test Suite (http://docs.aws.amazon.com/general/latest/gr/signature-v4-test-suite.html)
     * (get-vanilla.*)
     * <p>
     * GET / http/1.1
     * Date:Mon, 09 Sep 2011 23:36:00 GMT
     * Host:host.foo.com
     *
     * @throws Exception
     */
    @Test
    public void _01testSend() throws Exception {
        Map data = new HashMap<>();
        data.put("message", "test message");
        eventsClient.send(Types.DATASET_DOWNLOAD, data);
        eventsClient.refresh();
    }

    @Test
    public void _02testReceive() {
        try {
            List<SearchResult.Hit<EventItem, Void>> read = eventsClient.read(Types.DATASET_DOWNLOAD, DateUtils.addDays(new Date(), -10), DateUtils.addDays(new Date(), 1));
            Assert.assertNotNull(read);
            assertThat(read).hasSize(1);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void _03testReceiveAll() {
        try {
            List<SearchResult.Hit<EventItem, Void>> read = eventsClient.read(Types.DATASET_DOWNLOAD, null, null);
            Assert.assertNotNull(read);
            assertThat(read).hasSize(1);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void _04testReceiveWithEndDate() {
        try {
            List<SearchResult.Hit<EventItem, Void>> read = eventsClient.read(Types.DATASET_DOWNLOAD, null, DateUtils.addDays(new Date(), 1));
            Assert.assertNotNull(read);
            assertThat(read).hasSize(1);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void _05testReceiveWithStartDate() {
        try {
            List<SearchResult.Hit<EventItem, Void>> read = eventsClient.read(Types.DATASET_DOWNLOAD, DateUtils.addDays(new Date(), -10), DateUtils.addDays(new Date(), 1));
            Assert.assertNotNull(read);
            assertThat(read).hasSize(1);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private static EventsClient createClient() {
//        String awsAccessKey = "";
//        String awsSecretKey = "";
//        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
//        AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
//        String region = "us-east-1";
//        String service = "host";

        Map conf = new HashMap<>();
        conf.put("uri", "https://search-log-events-gqoyntjuhapanyx7j5kszghjoy.us-east-1.es.amazonaws.com");
        return EventsClientFactory.build(DestinationType.ELASTICSEARCH, Indexes.GHAP_TEST, conf);
    }

}
