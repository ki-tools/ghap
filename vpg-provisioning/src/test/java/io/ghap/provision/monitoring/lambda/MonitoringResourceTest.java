package io.ghap.provision.monitoring.lambda;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.guice.jetty.JettyConfig;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.guice.UnitTestTrackerModule;
import io.ghap.jetty.TestJettyModule;
import io.ghap.provision.vpg.Activity;
import io.ghap.provision.vpg.PersonalStorage;
import io.ghap.provision.vpg.VirtualPrivateGrid;
import io.ghap.provision.vpg.data.MonitoringResourceFactory;
import io.ghap.provision.vpg.data.PersonalStorageFactory;
import io.ghap.provision.vpg.data.VPGMultiFactory;
import io.ghap.provision.vpg.data.VirtualResource;
import io.ghap.provision.vpg.guice.VPGBootstrapModule;
import io.ghap.provision.vpg.guice.VPGServletModule;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

//@RunWith(GovernatorJunit4Runner.class)
//@LifecycleInjectorParams(modules = {VPGServletModule.class, TestJettyModule.class, UnitTestTrackerModule.class}, bootstrapModule = VPGBootstrapModule.class, scannedPackages = "io.ghap.provision.vpg")
public class MonitoringResourceTest {

    private static final String TEMPLATE_ANALYSIS_VPG_ACTIVITY = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-devtest/analysis-vpg-activity.json";
    private static final String TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-devtest/analysis-windows-activity_testv8e.json";

    private Logger logger = LoggerFactory.getLogger(MonitoringResourceTest.class);

    private final static long TIMEOUT = 1000 * 60 * 5;
    @Context
    SecurityContext securityContext;

    @Inject
    private MonitoringResource monitoringResource;

    @Inject
    private PersonalStorageFactory storageFactory;

    @Inject
    private VPGMultiFactory vpgFactory;

    @Inject
    private MonitoringResourceFactory monitoringResourceFactory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Configuration("aws.accessKeyId")
    private String keyId;

    @Configuration("aws.secretKey")
    private String secretKey;

    @Configuration("availability.zone")
    private String availabilityZone;

    @Inject
    private JettyConfig jettyConfig;

    @Before
    public void setup() {
/*
    System.setProperty("aws.accessKeyId", keyId);
    System.setProperty("aws.secretKey", secretKey);
*/
    }

    @After
    public void cleanupResources() {
        List<VirtualPrivateGrid> vpgs = vpgFactory.get();
        for (VirtualPrivateGrid vpg : vpgs) {
            vpgFactory.delete(vpg);
        }

        AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
        AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
        long start = System.currentTimeMillis();
        long sleep_time = 5;
        for (long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start) {
            try {
                try {
                    boolean abort = true;
                    for (VirtualPrivateGrid vpg : vpgs) {
                        String stackId = vpg.getStackId();
                        DescribeStacksRequest stackRequest = new DescribeStacksRequest();
                        stackRequest.setStackName(stackId);
                        DescribeStacksResult stackResults = cloudformationClient.describeStacks(stackRequest);
                        List<Stack> stacks = stackResults.getStacks();
                        String status = null;
                        for (Stack stack : stacks) {
                            status = stack.getStackStatus();
                            abort = abort && status.endsWith("_COMPLETE");
                        }
                    }
                    if (abort) {
                        break;
                    }
                    Thread.sleep(sleep_time);
                } catch (InterruptedException ie) {
                }
            } catch (AmazonServiceException ase) {
                if (ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling")) {
                    sleep_time *= 2;
                }
            }
        }
        List<PersonalStorage> storage = storageFactory.get();
        for (PersonalStorage entry : storage) {
            storageFactory.delete(entry);
        }

        for (VirtualPrivateGrid vpg : vpgs) {
            vpgFactory.deleteKey(vpg.getUserId());
        }
    }

    @Test
    public void testScheduleStopForIdleResources() throws IOException {
        Assert.assertTrue(vpgFactory.get().size() == 0);
        UUID userUuid = UUID.randomUUID();

        createPersonalStorageForUser(userUuid);

        Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);


        VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com", userUuid, activity);

        waitForGridToBeCreated(userUuid, activity);

        assertThatGridHasBeenCreated(userUuid, activity, grid);

        List<VirtualResource> virtualResources = vpgFactory.getVirtualResources(grid);
        assertNotNull("Expected virtual resources", virtualResources);
        assertFalse("Expected virtual resources", virtualResources.isEmpty());

        VirtualResource virtualResource = virtualResources.get(0);

        CloudWatchMessage cloudWatchMessage = createCloudWatchMessage(virtualResource.getInstanceId());



        CloseableHttpClient httpclient = createHttpClient();

        try {

            String url =
                    String.format("http://localhost:%d/rest/v1/Monitoring/scheduled-cleanup/schedule",
                            jettyConfig.getPort());

            Gson gson = createGson();
            String json = gson.toJson(cloudWatchMessage);
            StringEntity entity = new StringEntity(json);
            entity.setContentType("application/json");

            HttpPut put = new HttpPut(url);
            put.setEntity(entity);
            CloseableHttpResponse response = httpclient.execute(put);
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());

            assertFalse("Expected an entry in the monitored resource store", monitoringResourceFactory.list().isEmpty());

            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException ignore) {
            }


            waitForGridResourcesToBeStopped(grid);
            assertThatGridResourcesHaveBeenStopped(grid);

            assertTrue("Did not expect an entry in the monitored resource store", monitoringResourceFactory.list().isEmpty());


        } finally {
            try {
                httpclient.close();
            } catch (IOException ignored) {
            }

            vpgFactory.delete(userUuid, activity.getId());

            removePersonalStorageForUser(userUuid);
        }

    }

    private Gson createGson() {
        JsonDeserializer<DateTime> datetimeDeserializer = new DateTimeJsonAdapter();

        GsonBuilder builder  = new GsonBuilder();
        builder.registerTypeAdapter(DateTime.class, datetimeDeserializer);

        return builder.create();
    }


    private CloudWatchMessage createCloudWatchMessage(String instanceId) {
        Dimension dimension = new Dimension();
        dimension.name = "InstanceId";
        dimension.value = instanceId;

        Trigger trigger = new Trigger();
        trigger.MetricName = "CPUUtilization";
        trigger.Dimensions = Collections.singletonList(dimension);

        CloudWatchMessage cloudWatchMessage = new CloudWatchMessage();
        cloudWatchMessage.AlarmName = "Test Alarm Name";
        cloudWatchMessage.AlarmDescription = "Publish CPU usage";
        cloudWatchMessage.Trigger = trigger;
        cloudWatchMessage.NewStateValue = "ALARM";
        cloudWatchMessage.OldStateValue = "INSUFFICIENT_DATA";

        cloudWatchMessage.MessageTime = DateTime.now();
        cloudWatchMessage.MessageId = UUID.randomUUID().toString();



        return cloudWatchMessage;
    }

    @Test
    public void testCancelScheduledStopForIdleResources() throws IOException {
        Assert.assertTrue(vpgFactory.get().size() == 0);
        UUID userUuid = UUID.randomUUID();

        createPersonalStorageForUser(userUuid);

        Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);


        VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com", userUuid, activity);

        waitForGridToBeCreated(userUuid, activity);

        assertThatGridHasBeenCreated(userUuid, activity, grid);

        waitForGridResourcesToBeRunning(grid);

        List<VirtualResource> virtualResources = vpgFactory.getVirtualResources(grid);
        assertNotNull("Expected virtual resources", virtualResources);
        assertFalse("Expected virtual resources", virtualResources.isEmpty());

        VirtualResource virtualResource = virtualResources.get(0);

        CloudWatchMessage cloudWatchMessage = createCloudWatchMessage(virtualResource.getInstanceId());


        CloseableHttpClient httpclient = createHttpClient();

        try {

            String url =
                    String.format("http://localhost:%d/rest/v1/Monitoring/scheduled-cleanup/schedule",
                    jettyConfig.getPort());

            Gson gson = createGson();
            String json = gson.toJson(cloudWatchMessage);
            StringEntity entity = new StringEntity(json);
            entity.setContentType("application/json");

            HttpPut put = new HttpPut(url);
            put.setEntity(entity);
            CloseableHttpResponse response = httpclient.execute(put);
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());

            assertFalse("Expected an entry in the monitored resource store", monitoringResourceFactory.list().isEmpty());


            //For the test-cases, the job is scheduled to be kicked off in 15 seconds, so lets wait for 5 seconds and then
            //cancel the scheduled stop.
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException ignore) {
            }


            //Now cancel the schedeled cleanup.

            url = String.format("http://localhost:%d/rest/v1/Monitoring/scheduled-cleanup/cancel?token=%s",
                    jettyConfig.getPort(), cloudWatchMessage.MessageId);

            HttpGet get = new HttpGet(url);

            response = httpclient.execute(get);
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());


            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException ignore) {
            }


            assertThatGridResourcesAreRunning(grid);

            assertTrue("Did not expect an entry in the monitored resource store", monitoringResourceFactory.list().isEmpty());

        } finally {
            try {
                httpclient.close();
            } catch (IOException ignored) {
            }

            vpgFactory.delete(userUuid, activity.getId());

            removePersonalStorageForUser(userUuid);
        }

    }

    private CloseableHttpClient createHttpClient() {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope("localhost", jettyConfig.getPort()),
                new UsernamePasswordCredentials("tester", "testing"));

        return HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
    }

    private Activity createActivity(String activityName, String templateUrl) {
        Activity activity = new Activity();
        activity.setActivityName(activityName);
        activity.setId(UUID.randomUUID());
        activity.setDefaultComputationalUnits(1);
        activity.setMaximumComputationalUnits(2);
        activity.setMinimumComputationalUnits(1);
        activity.setTemplateUrl(templateUrl);

        return activity;
    }

    private void createPersonalStorageForUser(UUID userUuid) {
        PersonalStorage personalStorage = storageFactory.create(userUuid, 1);
        long start = System.currentTimeMillis();
        long sleep_time = 1;
        for (long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start) {
            try {
                boolean available = storageFactory.available(userUuid);
                if (available) {
                    break;
                }
            } catch (AmazonServiceException ase) {
                if (ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling")) {
                    sleep_time = sleep_time * 2;
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }

    }

    private void removePersonalStorageForUser(UUID userUuid) {
        int elapsed = 0;
        for (boolean available = storageFactory.available(userUuid); !available; available = storageFactory.available(userUuid)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                // don't care
            }
            elapsed += 1000;
            if (elapsed >= TIMEOUT) {
                break;
            }
        }
        storageFactory.delete(userUuid);

    }

    private void assertThatGridResourcesAreRunning(VirtualPrivateGrid grid) {
        List<VirtualResource> resources = vpgFactory.getVirtualResources(grid);
        for (VirtualResource resource : resources) {
            Assert.assertEquals("running", resource.getStatus());
        }
    }

    private void assertThatGridResourcesHaveBeenStopped(VirtualPrivateGrid grid) {
        List<VirtualResource> resources = vpgFactory.getVirtualResources(grid);
        for (VirtualResource resource : resources) {
            Assert.assertEquals("stopped", resource.getStatus());
        }
    }

    private void waitForGridResourcesToBeRunning(VirtualPrivateGrid grid) {
        long start = System.currentTimeMillis();

        int sleepTime = 1;

        for (long duration = 0; duration < TIMEOUT * 10; duration = System.currentTimeMillis() - start) {
            try {
                Boolean finished = null;
                for (VirtualResource resource : vpgFactory.getVirtualResources(grid)) {
                    if (finished == null) {
                        finished = resource.getStatus().equalsIgnoreCase("running");
                    } else {
                        finished = finished && resource.getStatus().equalsIgnoreCase("running");
                    }
                }
                if (finished) {
                    break;
                }
            } catch (AmazonServiceException ase) {
                if (ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling")) {
                    sleepTime = sleepTime * 2;
                }
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }
        }

    }

    private void waitForGridResourcesToBeStopped(VirtualPrivateGrid grid) {
        long start = System.currentTimeMillis();

        int sleepTime = 1;
        for (long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start) {
            try {
                boolean continue_checking = true;
                List<VirtualResource> resources = vpgFactory.getVirtualResources(grid);
                for (VirtualResource resource : resources) {
                    continue_checking = continue_checking && !resource.getStatus().equalsIgnoreCase("stopped");
                }
                if (!continue_checking) {
                    break;
                }
            } catch (AmazonServiceException ase) {
                if (ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling")) {
                    sleepTime = sleepTime * 2;
                }
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }
        }
    }

    private void assertThatGridHasBeenCreated(UUID userUuid, Activity activity, VirtualPrivateGrid grid) {
        Assert.assertNotNull("The grid could not be created", grid);

        Assert.assertEquals("CREATE_COMPLETE", vpgFactory.status(userUuid, activity));
    }

    private void waitForGridToBeCreated(UUID userUuid, Activity activity) {
        long start = System.currentTimeMillis();

        for (long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
            String status = vpgFactory.status(userUuid, activity);
            boolean done = status.endsWith("_COMPLETE");
            if (done) {
                break;
            }
        }

    }

    private static class DateTimeJsonAdapter implements JsonDeserializer<DateTime>, JsonSerializer<DateTime> {
        @Override
        public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
            return formatter.parseDateTime(json.getAsString());
        }

        @Override
        public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {
            final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
            return  new JsonPrimitive(formatter.print(src));

        }
    }
}