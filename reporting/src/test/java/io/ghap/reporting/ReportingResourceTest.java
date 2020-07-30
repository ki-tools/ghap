package io.ghap.reporting;

import com.github.hburgmeier.jerseyoauth2.api.user.IUser;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthPrincipal;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthSecurityContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.guice.jetty.JettyConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.guice.JerseyServletModule;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.oauth.OAuthUser;
import io.ghap.reporting.bootstrap.BasicBootstrap;
import io.ghap.reporting.data.*;
import io.ghap.reporting.guice.ReportingServletModule;
import io.ghap.reporting.guice.TestGrizzlyModule;
import io.ghap.reporting.service.HttpService;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

@RunWith(GovernatorJunit4Runner.class)
@LifecycleInjectorParams(modules = {ReportingServletModule.class, TestGrizzlyModule.class}, bootstrapModule = BasicBootstrap.class, scannedPackages = "io.ghap.reporting")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReportingResourceTest extends JerseyServletModule {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private static String token;
  private Set<String> tokens = new HashSet<>();

  @Inject
  private ReportResource reportResource;

  @Inject
  private ReportFactory reportFactory;

  @Inject
  private JettyConfig jettyConfig;

  @Inject
  private HttpService httpService;

  @BeforeClass
  public static void setUp() throws Exception {
    token = login();
  }

  @Test
  public void TestStatus() {
    try
    {
      String url = String.format("http://localhost:%d/rest/v1/Reporting/getstatus/%s?token=%s",
              jettyConfig.getPort(), UUID.randomUUID().toString(), ReportingResourceTest.token);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .get(ClientResponse.class);
      Assert.assertEquals(404, response.getStatus());
    }
    catch(Exception e)
    {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestCreateUserReport() {
    createReport(ReportType.USER_STATUS);
  }

  @Test
  public void TestCreateComputeReport() {
    createReport(ReportType.COMPUTE);
  }

  @Test
  public void TestCreateGrantReport() {
    createReport(ReportType.GRANT_STATUS);
  }

  @Test
  public void TestCreateGroupReport() {
    createReport(ReportType.GROUP_STATUS);
  }

  @Test
  public void TestCreateRoleReport() {
    createReport(ReportType.ROLE_STATUS);
  }

  @Test
  public void TestCreateWindowsComputeReport() {
    createReport(ReportType.WINDOWS_COMPUTE);
  }

  @Test
  public void TestCreateProgramReport() {
    createReport(ReportType.PROGRAM_STATUS);
  }

  @Test
  public void TestCreateConstrained() {
    try {
      UUID user = UUID.randomUUID();
      List<DateRangeConstraint> constraints = new ArrayList<DateRangeConstraint>();
      DateRangeConstraint constraint = new DateRangeConstraint();
      constraint.setConstraint(new DateRange(Calendar.getInstance().getTime(),
                                             Calendar.getInstance().getTime()));
      constraints.add(constraint);
      Gson gson = createGson();
      String jsonString = gson.toJson(constraints);
      String url = String.format("http://localhost:%d/rest/v1/Reporting/constrainedcreate/%s/%s",
              jettyConfig.getPort(), user, ReportType.COMPUTE);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .put(ClientResponse.class, jsonString);
      Assert.assertEquals(200, response.getStatus());
      InputStreamReader isr = new InputStreamReader(response.getEntityInputStream());
      String token = gson.fromJson(isr, String.class);
      tokens.add(token);
      Assert.assertNotNull(token);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetExistingStatus() {
    try
    {
      UUID user = UUID.randomUUID();
      String token = CreateReport(user);
      tokens.add(token);
      String url = String.format("http://localhost:%d/rest/v1/Reporting/getstatus/%s?token=%s",
              jettyConfig.getPort(), token, ReportingResourceTest.token);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .get(ClientResponse.class);
      Assert.assertEquals(200, response.getStatus());
    }
    catch(Exception e)
    {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetReport() {
    try
    {
      UUID user = UUID.randomUUID();
      String token = CreateReport(user);
      tokens.add(token);
      String url = String.format("http://localhost:%d/rest/v1/Reporting/getreport/%s?token=%s",
              jettyConfig.getPort(), token, ReportingResourceTest.token);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .get(ClientResponse.class);
      Assert.assertEquals(200, response.getStatus());
      InputStreamReader isr = new InputStreamReader(response.getEntityInputStream());
      Gson gson = createGson();
      Report report = gson.fromJson(isr, Report.class);
      Assert.assertNotNull(report);
      Assert.assertEquals(token, report.getToken());
      Assert.assertNotNull(report.getCreated());
      Assert.assertNotNull(report.getName());
      Assert.assertEquals(ReportType.USER_STATUS, report.getReportType());
      Assert.assertEquals(user, report.getOwner());
    }
    catch(Exception e)
    {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetUserReports() {
    try
    {
      UUID user = UUID.randomUUID();
      String token = CreateReport(user);
      tokens.add(token);
      String url = String.format("http://localhost:%d/rest/v1/Reporting/getuserreports/%s?token=%s",
              jettyConfig.getPort(), user, ReportingResourceTest.token);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .get(ClientResponse.class);
      Assert.assertEquals(200, response.getStatus());
      InputStreamReader isr = new InputStreamReader(response.getEntityInputStream());
      Gson gson = createGson();
      Report[] reports = gson.fromJson(isr, Report[].class);
      Assert.assertNotNull(reports);
      Assert.assertEquals(1, reports.length);
      for(Report report: reports) {
        Assert.assertEquals(token, report.getToken());
        Assert.assertEquals(token, report.getToken());
        Assert.assertNotNull(report.getCreated());
        Assert.assertNotNull(report.getName());
        Assert.assertEquals(ReportType.USER_STATUS, report.getReportType());
        Assert.assertEquals(user, report.getOwner());
      }
    }
    catch(Exception e)
    {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetAvailableReports() {
    try
    {
      String url = String.format("http://localhost:%d/rest/v1/Reporting/getavailablereports?token=%s",
              jettyConfig.getPort(), ReportingResourceTest.token);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .get(ClientResponse.class);
      Assert.assertEquals(200, response.getStatus());
      InputStreamReader isr = new InputStreamReader(response.getEntityInputStream());
      Gson gson = createGson();
      ReportDescriptor[] reportTypes = gson.fromJson(isr, ReportDescriptor[].class);
      Assert.assertNotNull(reportTypes);
      Assert.assertEquals(ReportType.values().length, reportTypes.length);
    }
    catch(Exception e)
    {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetReports() {
    try
    {
      UUID user = UUID.randomUUID();
      String token = CreateReport(user);
      String url = String.format("http://localhost:%d/rest/v1/Reporting/getreports?token=%s",
              jettyConfig.getPort(), ReportingResourceTest.token);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .get(ClientResponse.class);
      Assert.assertEquals(200, response.getStatus());
      Gson gson = createGson();
      InputStreamReader isr = new InputStreamReader(response.getEntityInputStream());
      Report[] reports = gson.fromJson(isr, Report[].class);
      Assert.assertNotNull(reports);
      for(Report report: reports) {
        Assert.assertNotNull(report.getToken());
        Assert.assertNotNull(report.getCreated());
        Assert.assertNotNull(report.getName());
      }
    }
    catch(Exception e)
    {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestRemoveReport() {
    try
    {
      UUID user = UUID.randomUUID();
      String token = CreateReport(user);
      String url = String.format("http://localhost:%d/rest/v1/Reporting/removereport/%s", jettyConfig.getPort(), token);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .delete(ClientResponse.class);
      Assert.assertEquals(200, response.getStatus());
    }
    catch(Exception e)
    {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void TestGetReportContentFromToken() {
    try
    {
      UUID user = UUID.randomUUID();
      String token = CreateReport(user);
      Assert.assertNotNull(token);
      Report report = reportFactory.GetReport(token);
      String url = String.format("http://localhost:%d/rest/v1/Reporting/getreportcontent/%s?token=%s",
              jettyConfig.getPort(), token, ReportingResourceTest.token);
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.type(MediaType.APPLICATION_OCTET_STREAM).get(ClientResponse.class);
      System.out.println("getreportcontent -> " + response.getStatus());
      Assert.assertEquals(200, response.getStatus());
      File res = response.getEntity(File.class);
      Assert.assertNotNull(res);
      Assert.assertTrue(res.length() > 0);
    }
    catch(Exception e)
    {
      log.error("Get report content exception.", e);
      Assert.fail(e.getMessage());
    }
  }

  @After
  public void CleanUp()
  {
    List<Report> reports = reportFactory.ListReports();
    for(Report report: reports) {
      for (String token : tokens) {
        if (report.getToken().equals(token)) {
          try {
            // try to prevent com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException
            Thread.sleep(100);
          } catch (InterruptedException e) {
            //ignore
          }
          reportFactory.RemoveReport(report.getToken());
          break;
        }
      }
    }
  }

  private Client createClient() {
    return httpService.create(token);
  }

  private Gson createGson() {
    GsonBuilder builder  = new GsonBuilder().setDateFormat("yyyy-MM-dd");
    return builder.create();
  }

  private String CreateReport(UUID uuid) {
    String token;
    try {
      token = reportFactory.CreateReport(ReportingResourceTest.token, uuid, ReportType.USER_STATUS);
      tokens.add(token);
    } catch (Throwable e) {
      log.error("Cannot create report for \"" + uuid + "\"", e);
      return null;
    }
    return token;
  }

  public static SecurityContext createSecurityContext(String token) {
    IUser user = new OAuthUser("test", "test@test.com", token);
    OAuthPrincipal principal = new OAuthPrincipal("projectservice", user, new HashSet<String>());
    return new OAuthSecurityContext(principal, false);
  }

  public static String login() throws IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException {
    AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
    String oauthUrl = configInstance.getString("oauth.admin.url");
    if (oauthUrl.endsWith("/oauth")) {
      oauthUrl = oauthUrl.substring(0, oauthUrl.length() - "/oauth".length());
    }
    String username = "GHAPAdministrator";//configInstance.getString("oauth.admin.username");
    String password = "";//configInstance.getString("oauth.admin.password");
    String sessionId = getSessionId();

    URL url = new URL(oauthUrl + "/j_spring_security_check");
    HttpsURLConnection conn  = (HttpsURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    conn.setInstanceFollowRedirects(false);
    conn.setDoOutput(true);
    conn.setRequestProperty("Cookie", "ppolicyread=read; JSESSIONID=" + sessionId);
    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
    String str = "j_username=" + URLEncoder.encode(username, "UTF-8") + "&j_password=" + URLEncoder.encode(password, "UTF-8");
    writer.write(str);
    writer.flush();
    writer.close();
    if (conn.getHeaderFields().containsKey("Set-Cookie")) {
      List<String> strings = conn.getHeaderFields().get("Set-Cookie");
      for (String header : strings) {
        if (header.contains("JSESSIONID")) {
          int endIndex = header.indexOf(";");
          sessionId = header.substring(header.indexOf("=") + 1, endIndex > 0 ? endIndex : header.length());
          break;
        }
      }
    }
    String location = conn.getHeaderField("Location");
    conn.disconnect();

    URL url2 = new URL(location);
    HttpsURLConnection conn2  = (HttpsURLConnection) url2.openConnection();
    conn2.setRequestMethod("GET");
    conn2.setRequestProperty("Cookie", "ppolicyread=read; JSESSIONID=" + sessionId);
    conn2.setInstanceFollowRedirects(false);
    location = conn2.getHeaderField("Location");
    conn2.disconnect();
    String fragment = new URIBuilder(location).getFragment();
    int access_token = fragment.indexOf("access_token") + "access_token".length() + 1;
    String token = fragment.substring(access_token, fragment.indexOf("&", access_token));
    return token;
  }

  public static String getSessionId() throws IOException, NoSuchAlgorithmException, KeyManagementException {
    TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
              }

              public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

              public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

            }
    };

    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

// Create all-trusting host name verifier
    HostnameVerifier allHostsValid = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };
// Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
    String oauthUrl = configInstance.getString("oauth.admin.url");
    URL url = new URL(oauthUrl + "/authorize?client_id=projectservice&response_type=token&redirect_uri=http://www.google.com");
    HttpsURLConnection conn  = (HttpsURLConnection) url.openConnection();
    conn.setInstanceFollowRedirects(false);
    String sessionId = conn.getHeaderField("Set-Cookie");
    sessionId = sessionId.substring(sessionId.indexOf("=") + 1, sessionId.indexOf(";"));
    conn.disconnect();
    return sessionId;
  }

  private static void addAuthHeader(HttpRequestBase request) {
    request.setHeader("Authorization", "Bearer " + token);
  }

  private void createReport(ReportType reportType) {
    try
    {

      UUID user = UUID.randomUUID();
      String url = String.format("http://localhost:%d/rest/v1/Reporting/create/%s/%s", jettyConfig.getPort(), user, reportType.name());
      WebResource resource = createClient().resource(url);
      ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
              .put(ClientResponse.class);
      Assert.assertEquals(200, response.getStatus());
      Gson gson = createGson();
      InputStreamReader isr = new InputStreamReader(response.getEntityInputStream());
      String token = gson.fromJson(isr, String.class);
      tokens.add(token);

      Assert.assertNotNull(token);
    }
    catch(Exception e)
    {
      Assert.fail(e.getMessage());
    }
  }
}
