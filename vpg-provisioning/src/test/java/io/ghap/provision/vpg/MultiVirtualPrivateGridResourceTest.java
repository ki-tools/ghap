package io.ghap.provision.vpg;

import com.amazonaws.AmazonServiceException;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.netflix.governator.guice.jetty.JettyConfig;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.jetty.TestJettyModule;
import io.ghap.provision.vpg.data.*;
import io.ghap.guice.UnitTestTrackerModule;
import io.ghap.provision.vpg.guice.VPGServletBootstrapModule;
import io.ghap.provision.vpg.guice.VPGServletModule;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.*;
import org.junit.rules.ExpectedException;

import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(GovernatorJunit4Runner.class)
@LifecycleInjectorParams(modules = { VPGServletModule.class, TestJettyModule.class, UnitTestTrackerModule.class}, bootstrapModule = VPGServletBootstrapModule.class, scannedPackages = "io.ghap.provision.vpg")
public class MultiVirtualPrivateGridResourceTest
{
  private static final String TEMPLATE_ANALYSIS_VPG_ACTIVITY = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-devtest/analysis-vpg-activity.json";
  private static final String TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-devtest/analysis-windows-activity_testv8e.json";

  private Logger logger = LoggerFactory.getLogger(MultiVirtualPrivateGridResourceTest.class);

  private final static long TIMEOUT = 1000 * 60 * 5;
  @Context SecurityContext securityContext;

  @Inject
  private MultiVirtualPrivateGridResource vpgResource;

  @Inject
  private VPGMultiFactory vpgFactory;

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

  @Test
  public void TestConfigurationBinding()
  {
    Assert.assertEquals(vpgResource.getServiceName(), "Test Multi Instance Virtual Private Grid Manager");
  }

  @Test
  public void TestDoesNotExist()
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    String uuid = UUID.randomUUID().toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    try {
      CloseableHttpClient httpclient = createHttpClient();

      try {
        String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/exists/%s", jettyConfig.getPort(), uuid);
        Gson gson = createGson();
        String json = gson.toJson(activity);
        StringEntity entity = new StringEntity(json);
        entity.setContentType("application/json");

        HttpPut put = new HttpPut(url);
        put.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(put);
        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
      } finally {
        httpclient.close();
      }
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestCreate() {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    String uuid = UUID.randomUUID().toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    try {
      CloseableHttpClient httpclient = createHttpClient();

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/create/%s", jettyConfig.getPort(), uuid);
      Gson gson = createGson();
      String json = gson.toJson(activity);
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      HttpPut put = new HttpPut(url);
      put.setEntity(entity);
      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      String status = "UNKNOWN";
      while(!status.endsWith("_COMPLETE")) {
        url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/status/%s", jettyConfig.getPort(), uuid);
        gson = createGson();
        json = gson.toJson(activity);
        entity = new StringEntity(json);
        entity.setContentType("application/json");
        put = new HttpPut(url);
        put.setEntity(entity);
        response = httpclient.execute(put);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
        status = gson.fromJson(isr, String.class);
        try {
          Thread.sleep(500);
        } catch(InterruptedException ie) {}
      }
      Assert.assertEquals("CREATE_COMPLETE", status);
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetCompute() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();

    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid vpg = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {

      start = System.currentTimeMillis();
      List validstates = new ArrayList<String>();
      validstates.add("initializing");
      validstates.add("running");

      for(long duration = 0; duration < TIMEOUT * 5; duration = System.currentTimeMillis() - start) {
        String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/get/compute/%s", jettyConfig.getPort(), vpg.getId());

        HttpGet get = new HttpGet(url);

        Gson gson = createGson();

        CloseableHttpResponse get_response = httpclient.execute(get);
        Assert.assertEquals(200, get_response.getStatusLine().getStatusCode());

        InputStreamReader isr = new InputStreamReader(get_response.getEntity().getContent());
        VirtualResource[] resources = gson.fromJson(isr, VirtualResource[].class);

        try { isr.close(); } catch(IOException ignore) {}

        Assert.assertEquals(2, resources.length);
        Assert.assertTrue(validstates.contains(resources[0].getStatus()));
        if(resources[0].getStatus().equals("running")) {
          break;
        }
      }
    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}


      vpgFactory.delete(userUuid, activity.getId());
    }
  }

  @Test
  public void TestCreateMultiple() {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    String uuid = UUID.randomUUID().toString();
    Activity activity_one = createActivity("Linux", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    Activity activity_two = createActivity("Windows 1", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    Activity activity_three = createActivity("Windows 2", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    Activity activity_four = createActivity("Windows 3", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    Activity[] activities = new Activity[] { activity_one, activity_two, activity_three, activity_four };

    try {
      CloseableHttpClient httpclient = createHttpClient();

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/create-multiple/%s", jettyConfig.getPort(), uuid);
      Gson gson = createGson();
      String json = gson.toJson(activities);
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      HttpPut put = new HttpPut(url);
      put.setEntity(entity);
      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
      VirtualPrivateGrid[] grids = gson.fromJson(isr, VirtualPrivateGrid[].class);
      Assert.assertEquals(4, grids.length);
      try {
        isr.close();
      } catch(IOException ioe) {}

      long start = System.currentTimeMillis();
      for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
        url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/statuses/%s", jettyConfig.getPort(), uuid);
        json = gson.toJson(activities);
        entity = new StringEntity(json);
        entity.setContentType("application/json");

        put = new HttpPut(url);
        put.setEntity(entity);
        response = httpclient.execute(put);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        isr = new InputStreamReader(response.getEntity().getContent());
        ActivityStatus[] statuses = gson.fromJson(isr, ActivityStatus[].class);
        try {
          isr.close();
        } catch(IOException ioe) {}

        boolean done = true;
        for(ActivityStatus status: statuses) {
          done = done && status.getStatus().endsWith("_COMPLETE");
        }
        if(done) {
          for(ActivityStatus status: statuses) {
            Assert.assertEquals("CREATE_COMPLETE", status.getStatus());
          }
          break;
        }
      }
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetGridInfoByUserAndActivity() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);


    VirtualPrivateGrid createdGrid = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {
      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/get-by-activity/%s", jettyConfig.getPort(), uuid);

      Gson gson = createGson();
      String json = gson.toJson(activity);
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      HttpPut put = new HttpPut(url);
      put.setEntity(entity);
      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
      VirtualPrivateGrid vpg = gson.fromJson(isr, VirtualPrivateGrid.class);

      try {
        isr.close();
      } catch(IOException ignored) {}


      Assert.assertNotNull("Expected grid", vpg);
      Assert.assertEquals("Unexpected activity identifier for grid", activity.getId(), vpg.getActivityId());


    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }
  }

  @Test
  public void TestGetRdpStreamForGrid() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);


    VirtualPrivateGrid createdGrid = vpgFactory.create("tester","test-user@nowhere.com", userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {
      List<VirtualResource> virtualResources = vpgFactory.getVirtualResources(createdGrid);
      Assert.assertNotNull("Expected some virtual resources in the grid", virtualResources);
      Assert.assertFalse("Expected some virtual resources in the grid", virtualResources.isEmpty());


      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/rdp", jettyConfig.getPort());

      Gson gson = createGson();
      String json = gson.toJson(virtualResources.get(0));
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      HttpPut put = new HttpPut(url);
      put.setEntity(entity);
      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
      String rdpContent = CharStreams.toString(isr);
      try {
        isr.close();
      } catch(IOException ignored) {}


      Assert.assertNotNull("Expected rdp information", rdpContent);
      Assert.assertFalse("Did not expect rdp information to be empty", rdpContent.isEmpty());
    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }

  @Test
  public void TestRdp()
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    String uuid = UUID.randomUUID().toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    try {
      CloseableHttpClient httpclient = createHttpClient();

      try {
        String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/create/%s", jettyConfig.getPort(), uuid);
        Gson gson = createGson();
        String json = gson.toJson(activity);
        StringEntity entity = new StringEntity(json);
        entity.setContentType("application/json");

        HttpPut put = new HttpPut(url);
        put.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(put);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
        VirtualPrivateGrid vpg = gson.fromJson(isr, VirtualPrivateGrid.class);
        isr.close();

        String status = "UNKNOWN";
        while(!status.endsWith("_COMPLETE")) {
          url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/status/%s", jettyConfig.getPort(), uuid);
          gson = new Gson();
          json = gson.toJson(activity);
          entity = new StringEntity(json);
          entity.setContentType("application/json");
          put = new HttpPut(url);
          put.setEntity(entity);
          response = httpclient.execute(put);
          Assert.assertEquals(200, response.getStatusLine().getStatusCode());
          isr = new InputStreamReader(response.getEntity().getContent());
          status = gson.fromJson(isr, String.class);
          try {
            Thread.sleep(500);
          } catch(InterruptedException ie) {}
        }
        Assert.assertEquals("CREATE_COMPLETE", status);
        url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/get/compute/%s", jettyConfig.getPort(), vpg.getId());
        HttpGet get = new HttpGet(url);
        response = httpclient.execute(get);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        isr = new InputStreamReader(response.getEntity().getContent());
        VirtualResource[] resources = gson.fromJson(isr, VirtualResource[].class);
        Assert.assertEquals(1, resources.length);
        VirtualResource resource = resources[0];
        url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/rdp-file/%s/%s?ipAddress=%s",
            jettyConfig.getPort(),
            resource.getInstanceId(),
            resource.getInstanceOsType(),
            resource.getAddress());

        get = new HttpGet(url);
        response = httpclient.execute(get);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        Assert.assertTrue(response.getEntity().getContentLength() > 0);
      } finally {
        String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/terminate/%s/%s", jettyConfig.getPort(), uuid, activity.getId());

        HttpDelete delete = new HttpDelete(url);
        CloseableHttpResponse response = httpclient.execute(delete);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        httpclient.close();
      }
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetPemStreamForGrid() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);


    vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/pem/%s/%s", jettyConfig.getPort(), uuid, activity.getId());

      HttpGet get = new HttpGet(url);
      CloseableHttpResponse response = httpclient.execute(get);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
      String pemKeyContent = CharStreams.toString(isr);
      try {
        isr.close();
      } catch(IOException ignored) {}


      Assert.assertNotNull("Expected pem key information", pemKeyContent);
      Assert.assertFalse("Did not expect pem key information to be empty", pemKeyContent.isEmpty());


    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }

  @Test
  public void TestGetAllGrids() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);
    vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/get", jettyConfig.getPort());

      Gson gson = createGson();

      HttpGet put = new HttpGet(url);
      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());

      VirtualPrivateGrid[] virtualPrivateGrids = gson.fromJson(isr, VirtualPrivateGrid[].class);

      try {
        isr.close();
      } catch(IOException ignored) {}


      Assert.assertNotNull("Expected grids", virtualPrivateGrids);
      Assert.assertEquals("Unexpected number of grids", 1, virtualPrivateGrids.length);

      Assert.assertEquals("Unexpected activity associated to grid", activity.getId(), virtualPrivateGrids[0].getActivityId());

    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }

  @Test
  public void TestGetGridsForUser() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);
    vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/get/%s", jettyConfig.getPort(), uuid);

      Gson gson = createGson();

      HttpGet put = new HttpGet(url);
      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());

      VirtualPrivateGrid[] virtualPrivateGrids = gson.fromJson(isr, VirtualPrivateGrid[].class);

      try {
        isr.close();
      } catch(IOException ignored) {}


      Assert.assertNotNull("Expected grids", virtualPrivateGrids);
      Assert.assertEquals("Unexpected number of grids", 1, virtualPrivateGrids.length);

      Assert.assertEquals("Unexpected activity associated to grid", activity.getId(), virtualPrivateGrids[0].getActivityId());

    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }

  @Test
  public void TestGetComputeResourcesForUser() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/get/user/compute/%s", jettyConfig.getPort(), uuid);

      Gson gson = createGson();

      HttpGet put = new HttpGet(url);
      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());

      VirtualResource[] virtualResources = gson.fromJson(isr, VirtualResource[].class);

      try {
        isr.close();
      } catch(IOException ignored) {}


      Assert.assertNotNull("Expected compute resource", virtualResources);
      Assert.assertEquals("Unexpected number of compute resources", 2, virtualResources.length);

      Assert.assertEquals("Unexpected grid associated with resource", grid.getId(), virtualResources[0].getVpgId());

    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }

  @Test
  public void TestGetComputeResourcesForGrid() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/get/compute/%s", jettyConfig.getPort(), grid.getId());

      Gson gson = createGson();

      HttpGet put = new HttpGet(url);
      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());

      VirtualResource[] virtualResources = gson.fromJson(isr, VirtualResource[].class);

      try {
        isr.close();
      } catch(IOException ignored) {}


      Assert.assertNotNull("Expected compute resources", virtualResources);
      Assert.assertEquals("Unexpected number of compute resources", 2, virtualResources.length);

      Assert.assertEquals("Unexpected grid associated with resource", grid.getId(), virtualResources[0].getVpgId());

    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }

  @Test
  public void TestCheckIfStackExistsForUser() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/exists/%s", jettyConfig.getPort(), uuid);

      Gson gson = createGson();

      HttpPut put = new HttpPut(url);

      String json = gson.toJson(activity);
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      put.setEntity(entity);

      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }

  @Test
  public void TestExistencesOfStacksForUserActivities() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);


    VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {

      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/existences/%s", jettyConfig.getPort(), uuid);

      Gson gson = createGson();

      HttpPut put = new HttpPut(url);

      String json = gson.toJson(Collections.singletonList(activity));
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      put.setEntity(entity);

      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());

      ActivityExistence[] activityExistences = gson.fromJson(isr, ActivityExistence[].class);

      try {
        isr.close();
      } catch(IOException ignored) {}


      Assert.assertNotNull("Expected activity existence states", activityExistences);
      Assert.assertEquals("Unexpected number of activity existence states", 1, activityExistences.length);

      Assert.assertEquals("Unexpected activity associated with resource", activity.getId(), activityExistences[0].getActivityId());

      Assert.assertTrue("Expected activity to exist", activityExistences[0].getExistence());

    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }

  @Test
  public void TestGetConsole() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();
    Activity activity = createActivity("Blah", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    CloseableHttpClient httpclient = createHttpClient();

    try {
      List<VirtualResource> virtualResources = vpgFactory.getVirtualResources(grid);
      Assert.assertNotNull("Expected some virtual resources in the grid", virtualResources);
      Assert.assertFalse("Expected some virtual resources in the grid", virtualResources.isEmpty());


      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/console", jettyConfig.getPort());

      Gson gson = createGson();

      HttpPut put = new HttpPut(url);

      String json = gson.toJson(virtualResources.get(0));
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      put.setEntity(entity);

      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());

      JsonReader jsonReader = new JsonReader(new InputStreamReader(response.getEntity().getContent()));
      //Some type of character (maybe whitespace, EOL) in console contents is causing a malformed json exception.
      //Setting lenient option to true to avoid this
      jsonReader.setLenient(true);


      String consoleContent = gson.fromJson(jsonReader, String.class);

      try {
        jsonReader.close();
      } catch(IOException ignored) {}

      Assert.assertNotNull("Expected console content", consoleContent);

    } finally {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }


  @Test
  public void TestPauseAndResume() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);


    VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

    Assert.assertNotNull("The grid could not be created", grid);

    Assert.assertEquals("CREATE_COMPLETE", vpgFactory.status(userUuid, activity));


    CloseableHttpClient httpclient = createHttpClient();

    try
    {
      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/pause/%s", jettyConfig.getPort(), uuid);

      Gson gson = createGson();

      HttpPut put = new HttpPut(url);

      String json = gson.toJson(activity);
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      put.setEntity(entity);

      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());


      int sleepTime = 1;
      for(long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start) {
        try {
          boolean continue_checking = true;
          List<VirtualResource> resources = vpgFactory.getVirtualResources(grid);
          for (VirtualResource resource : resources) {
            continue_checking = continue_checking && !resource.getStatus().equalsIgnoreCase("stopped");
          }
          if(!continue_checking) {
            break;
          }
        }
        catch(AmazonServiceException ase) {
          if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
          {
            sleepTime = sleepTime * 2;
          }
        }
        try { Thread.sleep(sleepTime);} catch(InterruptedException ie) {}
      }


      List<VirtualResource> resources = vpgFactory.getVirtualResources(grid);
      for(VirtualResource resource: resources) {
        Assert.assertEquals("stopped", resource.getStatus());
      }


      //Now test the Resume aspect
      url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/resume/%s", jettyConfig.getPort(), uuid);
      put = new HttpPut(url);

      json = gson.toJson(activity);
      entity = new StringEntity(json);
      entity.setContentType("application/json");

      put.setEntity(entity);

      response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());


      sleepTime = 1;
      for(long duration = 0; duration < TIMEOUT*10; duration = System.currentTimeMillis() - start) {
        try {
          Boolean finished = null;
          for (VirtualResource resource : vpgFactory.getVirtualResources(grid)) {
            if(finished == null) {
              finished = resource.getStatus().equalsIgnoreCase("running");
            } else {
              finished = finished && resource.getStatus().equalsIgnoreCase("running");
            }
          }
          if(finished) {
            break;
          }
        }
        catch(AmazonServiceException ase) {
          if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
          {
            sleepTime = sleepTime * 2;
          }
        }
        try { Thread.sleep(sleepTime);} catch(InterruptedException ie) {}
      }

      for(VirtualResource resource: vpgFactory.getVirtualResources(grid)) {
        Assert.assertEquals("running", resource.getStatus());
      }
    } finally
    {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }
  @Test
  public void TestPause() throws IOException
  {
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID userUuid = UUID.randomUUID();
    String uuid = userUuid.toString();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);


    VirtualPrivateGrid grid = vpgFactory.create("tester", "test-user@nowhere.com",userUuid, activity);

    waitForGridToBeCreated(userUuid, activity);

    assertThatGridHasBeenCreated(userUuid, activity, grid);


    CloseableHttpClient httpclient = createHttpClient();

    try
    {
      String url = String.format("http://localhost:%d/rest/v1/MultiVirtualPrivateGrid/pause/%s", jettyConfig.getPort(), uuid);

      Gson gson = createGson();

      HttpPut put = new HttpPut(url);

      String json = gson.toJson(activity);
      StringEntity entity = new StringEntity(json);
      entity.setContentType("application/json");

      put.setEntity(entity);

      CloseableHttpResponse response = httpclient.execute(put);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());


      waitForGridResourcesToBeStopped(grid);
      assertThatGridResourcesHaveBeenStopped(grid);



    } finally
    {
      try
      {
        httpclient.close();
      } catch (IOException ignored) {}

      vpgFactory.delete(userUuid, activity.getId());
    }

  }


  private CloseableHttpClient createHttpClient()
  {
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        new AuthScope("localhost", jettyConfig.getPort()),
        new UsernamePasswordCredentials("tester", "testing"));

    return HttpClients.custom()
        .setDefaultCredentialsProvider(credsProvider)
        .build();
  }

  private Activity createActivity(String activityName, String templateUrl)
  {
    Activity activity = new Activity();
    activity.setActivityName(activityName);
    activity.setId(UUID.randomUUID());
    activity.setDefaultComputationalUnits(1);
    activity.setMaximumComputationalUnits(2);
    activity.setMinimumComputationalUnits(1);
    activity.setTemplateUrl(templateUrl);

    return activity;
  }

  private void assertThatGridResourcesAreRunning(VirtualPrivateGrid grid)
  {
    List<VirtualResource> resources = vpgFactory.getVirtualResources(grid);
    for(VirtualResource resource: resources) {
      Assert.assertEquals("running", resource.getStatus());
    }
  }

  private void assertThatGridResourcesHaveBeenStopped(VirtualPrivateGrid grid)
  {
    List<VirtualResource> resources = vpgFactory.getVirtualResources(grid);
    for(VirtualResource resource: resources) {
      Assert.assertEquals("stopped", resource.getStatus());
    }
  }

  private void waitForGridResourcesToBeRunning(VirtualPrivateGrid grid)
  {
    long start = System.currentTimeMillis();

    int sleepTime = 1;

    for(long duration = 0; duration < TIMEOUT*10; duration = System.currentTimeMillis() - start) {
      try {
        Boolean finished = null;
        for (VirtualResource resource : vpgFactory.getVirtualResources(grid)) {
          if(finished == null) {
            finished = resource.getStatus().equalsIgnoreCase("running");
          } else {
            finished = finished && resource.getStatus().equalsIgnoreCase("running");
          }
        }
        if(finished) {
          break;
        }
      }
      catch(AmazonServiceException ase) {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleepTime = sleepTime * 2;
        }
      }
      try { Thread.sleep(sleepTime);} catch(InterruptedException ie) {}
    }

  }

  private void waitForGridResourcesToBeStopped(VirtualPrivateGrid grid)
  {
    long start = System.currentTimeMillis();

    int sleepTime = 1;
    for(long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start) {
      try {
        boolean continue_checking = true;
        List<VirtualResource> resources = vpgFactory.getVirtualResources(grid);
        for (VirtualResource resource : resources) {
          continue_checking = continue_checking && !resource.getStatus().equalsIgnoreCase("stopped");
        }
        if(!continue_checking) {
          break;
        }
      }
      catch(AmazonServiceException ase) {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleepTime = sleepTime * 2;
        }
      }
      try { Thread.sleep(sleepTime);} catch(InterruptedException ie) {}
    }
  }

  private void assertThatGridHasBeenCreated(UUID userUuid, Activity activity, VirtualPrivateGrid grid)
  {
    Assert.assertNotNull("The grid could not be created", grid);

    Assert.assertEquals("CREATE_COMPLETE", vpgFactory.status(userUuid, activity));
  }

  private void waitForGridToBeCreated(UUID userUuid, Activity activity)
  {
    long start = System.currentTimeMillis();

    for(long duration = 0; duration < TIMEOUT * 3; duration = System.currentTimeMillis() - start) {
      String status = vpgFactory.status(userUuid, activity);
      boolean done = status.endsWith("_COMPLETE");
      if(done) {
        break;
      }
    }

  }

  @After
  public void cleanup()
  {
    ((VPGMultiFactoryMockImpl)vpgFactory).cleanup();
  }

  @Before
  public void setup()
  {
    ((VPGMultiFactoryMockImpl)vpgFactory).setup();
  }

  private Gson createGson() {
    GsonBuilder builder  = new GsonBuilder();
    return builder.create();
  }

}
