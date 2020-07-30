package tests;

import com.github.hburgmeier.jerseyoauth2.api.user.IUser;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthPrincipal;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthSecurityContext;
import com.google.inject.Inject;
import com.netflix.config.ConfigurationManager;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.activity.bannermanagement.dao.CommonPersistDao;
import io.ghap.activity.bannermanagement.manager.BannerService;
import io.ghap.activity.bannermanagement.model.ApiBanner;
import io.ghap.activity.bannermanagement.model.ApiCreateBanner;
import io.ghap.activity.bannermanagement.model.ApiUpdateBanner;
import io.ghap.oauth.OAuthUser;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(GovernatorJunit4Runner.class)
@LifecycleInjectorParams(modules = {ProjectServiceModule.class}, bootstrapModule = Bootstrap.class, scannedPackages = "io.ghap.web.tests")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BannerServiceModuleTest {


    private static UUID projectId;
    private static String token;

    private static final String TEST_NAME = "TESTQWE";
    private static final String NEW_DESCRIPTION = "new description";

    @Inject
    private static BannerService bannerService;

    @Inject
    private CommonPersistDao commonPersistDao;

    @BeforeClass
    public static void setUp() throws Exception {

        token = login();
        ApiCreateBanner project = createBanner();
        ApiBanner result = bannerService.create(createSecurityContext(token), project);
        projectId = result.getId();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            bannerService.delete(projectId);
        } catch (Throwable e) {
        }
    }

    @Test
    public void _001getProjects() {
        bannerService.getAll();
        List<? extends ApiBanner> projects = bannerService.getAll();
        assertTrue(projects.size() > 0);
    }

    @Test
    public void _004deleteProject() {
        ApiCreateBanner project = createBanner("qweDSA", "qweDSA", "qweDSA");
        ApiBanner apiBanner = bannerService.create(createSecurityContext(token), project);
        List<? extends ApiBanner> projects = bannerService.getAll();
        boolean found = false;
        for (ApiBanner p : projects) {
            if (p.getId().equals(apiBanner.getId())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new RuntimeException("banner was not created");
        }

        bannerService.delete(apiBanner.getId());

        projects = bannerService.getAll();
        for (ApiBanner p : projects) {
            if (p.getId().equals(apiBanner.getId())) {
                throw new RuntimeException("banner was not deleted");
            }
        }
    }

    @Test
    public void _005updateProject() {
        ApiUpdateBanner project = new ApiUpdateBanner();
        project.setId(projectId);
        project.setTitle(TEST_NAME);
        project.setMessage(NEW_DESCRIPTION);
        ApiBanner result = bannerService.update(project);
        assertEquals(NEW_DESCRIPTION, result.getMessage());
    }

    public static SecurityContext createSecurityContext(String token) {
        IUser user = new OAuthUser("test", "test@test.com", token);
        OAuthPrincipal principal = new OAuthPrincipal("projectservice", user, new HashSet<String>());
        return new OAuthSecurityContext(principal, false);
    }

    public static String login() throws IOException, URISyntaxException {
        AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
        String oauthUrl = configInstance.getString("oauth.admin.url");
        String username = configInstance.getString("oauth.login");
        String password = configInstance.getString("oauth.password");
        String sessionId = getSessionId();

        CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build();
        HttpPost post = new HttpPost(oauthUrl + "j_spring_security_check");
        post.setHeader("Cookie", "JSESSIONID=" + sessionId);
        List<NameValuePair> list = new ArrayList<>(3);
        list.add(new BasicNameValuePair("j_username", username));
        list.add(new BasicNameValuePair("j_password", password));
        list.add(new BasicNameValuePair("user-policy", "on"));
        post.setEntity(new UrlEncodedFormEntity(list));
        CloseableHttpResponse execute = httpClient.execute(post);
        int statusCode = execute.getStatusLine().getStatusCode();
        Header[] locations = execute.getHeaders("location");
        Header[] headers = execute.getHeaders("Set-Cookie");
        for (Header header : headers) {
            sessionId = header.getValue();
            if (!sessionId.contains("JSESSIONID")) {
                continue;
            }
            sessionId = sessionId.substring(sessionId.indexOf("=") + 1, sessionId.indexOf(";"));
        }

        httpClient = HttpClientBuilder.create().disableRedirectHandling().build();
        HttpGet get = new HttpGet(locations[0].getValue());
        get.setHeader("Cookie", "ppolicyread=read; JSESSIONID=" + sessionId);
        execute = httpClient.execute(get);
        statusCode = execute.getStatusLine().getStatusCode();
        locations = execute.getHeaders("location");
        String fragment = new URIBuilder(locations[0].getValue()).getFragment();


        int access_token = fragment.indexOf("access_token") + "access_token".length() + 1;
        String token = fragment.substring(access_token, fragment.indexOf("&", access_token));
        return token;
    }

    public static String getSessionId() throws IOException {
        AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
        String oauthUrl = configInstance.getString("oauth.admin.url");
        URL url = new URL(oauthUrl + "oauth/authorize?client_id=projectservice&response_type=token&redirect_uri=http://www.google.com");
        HttpURLConnection conn  = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        byte[] buffer = new byte[8192];
        InputStream inputStream = conn.getInputStream();
        while (inputStream.read(buffer) > 0) {
            System.out.println(buffer);
        }
        String sessionId = conn.getHeaderField("Set-Cookie");
        sessionId = sessionId.substring(sessionId.indexOf("=") + 1, sessionId.indexOf(";"));
        conn.disconnect();
        return sessionId;
    }

    public static ApiCreateBanner createBanner() {
        return createBanner(TEST_NAME, TEST_NAME, TEST_NAME);
    }

    public static ApiCreateBanner createBanner(String name, String key, String description) {
        ApiCreateBanner project = new ApiCreateBanner();
        project.setTitle(name);
        project.setMessage(description);
        return project;
    }
}
