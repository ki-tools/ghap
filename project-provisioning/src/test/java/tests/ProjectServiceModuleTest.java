package tests;

import com.github.hburgmeier.jerseyoauth2.api.user.IUser;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthPrincipal;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthSecurityContext;
import com.google.inject.Inject;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.NotFoundException;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.oauth.OAuthUser;
import io.ghap.project.dao.CommonPersistDao;

import io.ghap.project.dao.StashProjectDao;
import io.ghap.project.dao.UserManagementDao;
import io.ghap.project.dao.impl.PermissionsDao;
import io.ghap.project.dao.impl.StubUserManagementDao;
import io.ghap.project.domain.*;
import io.ghap.project.exception.ApplicationException;
import io.ghap.project.manager.ProjectService;
import io.ghap.project.model.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.SecurityContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(GovernatorJunit4Runner.class)
@LifecycleInjectorParams(modules = {ProjectServiceModule.class}, bootstrapModule = Bootstrap.class, scannedPackages = "io.ghap.web.tests")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProjectServiceModuleTest {


    private static UUID projectId;
    private static UUID grantId;
    private static String token;
    private static UUID userId;

    private static final String TEST_NAME = "TESTQWE";
    private static final String NEW_PROJECT_KEY = "TESTQWE123";
    private static final String NEW_DESCRIPTION = "new description";
    private static final String NEW_GRANT_NAME = "TEST2QWE";
    private static final StashProviderImpl stashProvider = new StashProviderImpl();

    @Inject
    private static ProjectService projectService;

    @Inject
    private CommonPersistDao commonPersistDao;
    @Inject
    private PermissionsDao permissionsDao;
    @Inject
    private static UserManagementDao userManagementDao;

    @BeforeClass
    public static void setUp() throws Exception {
        stashProvider.setUp();
        token = login();
        ApiCreateProject project = createProject();
        ApiProject result = projectService.createProject(createSecurityContext(token), project);
        projectId = result.getId();
        ApiCreateGrant grant = new ApiCreateGrant();
        grant.setName(TEST_NAME);
        ApiGrant apiGrant = projectService.createGrant(createSecurityContext(token), grant, projectId);
        grantId = apiGrant.getId();

        Set<LdapUser> usersForGroup = userManagementDao.getUsersForGroup(token);
        for (LdapUser user : usersForGroup) {
            if (user.getName().equals("GHAPAdministrator")) {
                userId = user.getGuid();
                break;
            }
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            projectService.deleteProject(projectId);
        } catch (Throwable e) {
        }
        stashProvider.cleanUp();
    }

    @Test
    public void _001getProjects() {
        projectService.getAllProjects();
        Set<ApiProject> projects = projectService.getAllProjects();
        assertTrue(projects.size() > 0);
    }

    @Test
    public void _002getAllGrants() {
        Set<ApiGrant> apiGrants = projectService.getAllGrants(createSecurityContext(token), projectId);
        assertTrue(apiGrants.size() > 0);
    }

    @Test
    public void _003deleteGrant() {
        ApiCreateGrant grant = new ApiCreateGrant();
        grant.setName("grantForDelete");
        ApiGrant apiGrant = projectService.createGrant(createSecurityContext(token), grant, projectId);
        projectService.deleteGrant(apiGrant.getId());
        Set<ApiGrant> apiGrants = projectService.getAllGrants(createSecurityContext(token), projectId);
        for (ApiGrant g : apiGrants) {
            if (grant.getName().equals(g.getName())) {
                throw new RuntimeException("grant was not deleted");
            }
        }
    }

    @Test
    public void _004deleteProject() {
        ApiCreateProject project = createProject("qweDSA", "qweDSA", "qweDSA");
        ApiProject apiProject = projectService.createProject(createSecurityContext(token), project);
        Set<ApiProject> projects = projectService.getAllProjects();
        boolean found = false;
        for (ApiProject p : projects) {
            if (p.getId().equals(apiProject.getId())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new RuntimeException("project was not created");
        }

        projectService.deleteProject(apiProject.getId());

        projects = projectService.getAllProjects();
        for (ApiProject p : projects) {
            if (p.getId().equals(apiProject.getId())) {
                throw new RuntimeException("project was not deleted");
            }
        }
    }

    @Test
    public void _005updateProject() {
        ApiUpdateProject project = new ApiUpdateProject();
        project.setId(projectId);
        project.setKey(TEST_NAME);
        project.setName(TEST_NAME);
        project.setDescription(NEW_DESCRIPTION);
        ApiProject result = projectService.updateProject(project);
        assertEquals(NEW_DESCRIPTION, result.getDescription());
    }

    @Test
    public void _006updateGrant() {
        ApiUpdateGrant grant = new ApiUpdateGrant();
        grant.setId(grantId);
        grant.setName(NEW_GRANT_NAME);
        ApiGrant result = projectService.updateGrant(createSecurityContext(token), grant);
        assertEquals(NEW_GRANT_NAME, result.getName());
    }


    @Test(expected = NotFoundException.class)
    public void _007createGrantByNotExistsProject() {
        ApiCreateGrant grant = new ApiCreateGrant();
        grant.setName("grantForDelete");
        projectService.createGrant(createSecurityContext(token), grant, UUID.randomUUID());
    }

    @Test(expected = NotFoundException.class)
    public void _008getGrantsByNotExistsProject() {
        projectService.getAllGrants(createSecurityContext(token), UUID.randomUUID());
    }

    @Test(expected = NotFoundException.class)
    public void _009deleteNotExistGrant() {
        projectService.deleteGrant(UUID.randomUUID());
    }

    @Test(expected = ApplicationException.class)
    public void _010createSameGrant() {
        ApiCreateGrant grant = new ApiCreateGrant();
        grant.setName(NEW_GRANT_NAME);
        projectService.createGrant(createSecurityContext(token), grant, projectId);
    }

    @Test(expected = ApplicationException.class)
    public void _011createSameProject() {
        ApiCreateProject project = createProject();
        projectService.createProject(createSecurityContext(token), project);
    }

    @Test
    public void _012grantProjectPermissions() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ);
        projectService.grantProjectPermissions(createSecurityContext(token), userId, projectId, read);

        Set<PermissionRule> userProjectPermissions = projectService.getUserProjectPermissions(userId, projectId);
        Assert.assertTrue(CollectionUtils.isEqualCollection(userProjectPermissions, read));
    }

    @Test
    public void _013getProjectByUser() {
        Set<ApiProject> projects = projectService.getAllProjects(userId);
        assertTrue(projects.size() > 0);
    }

    @Test
    public void _014getPermissionsByProject() {
        Set<PermissionRule> rules = (Set<PermissionRule>) projectService.getUserProjectPermissions(userId, projectId);
        assertTrue(rules.size() > 0);
    }

    @Test(expected = NotFoundException.class)
    public void _015getPermissionsByNotExistsProject() {
        List<Permission> permissions = permissionsDao.loadPermissions(commonPersistDao.read(Project.class, projectId));
        User admin = permissions.get(0).getUser();
        Set<PermissionRule> rules = (Set<PermissionRule>) projectService.getUserProjectPermissions(admin.getId(), UUID.randomUUID());
    }

    @Test(expected = NotFoundException.class)
    public void _016getPermissionsByNotExistsUser() {
        List<Permission> permissions = permissionsDao.loadPermissions(commonPersistDao.read(Project.class, projectId));
        projectService.getUserProjectPermissions(UUID.randomUUID(), projectId);
    }

    @Test
    public void _017loadUsersForProject() {
        Set<LdapUser> projectUsers = projectService.getProjectUsers(createSecurityContext(token), projectId);
        boolean found = false;
        for (LdapUser u : projectUsers) {
            if (u.getGuid().equals(userId)) {
                found = true;
                break;
            }
        }
    }

    @Test(expected = NotFoundException.class)
    public void _018loadUsersForNotExistProject() {
        Set<LdapUser> projectUsers = projectService.getProjectUsers(createSecurityContext(token), UUID.randomUUID());
        boolean found = false;
        for (LdapUser u : projectUsers) {
            if (u.getGuid().equals(userId)) {
                found = true;
                break;
            }
        }
    }

    @Test
    public void _019revokeProjectPermissions() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ);
        projectService.revokeProjectPermissions(createSecurityContext(token), userId, projectId, read);

        Set<PermissionRule> userProjectPermissions = projectService.getUserProjectPermissions(userId, projectId);
        Assert.assertTrue(userProjectPermissions.isEmpty());
    }

    @Test
    public void _020grantProjectPermissions2() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.grantProjectPermissions(createSecurityContext(token), userId, projectId, read);

        Set<PermissionRule> userProjectPermissions = projectService.getUserProjectPermissions(userId, projectId);
        Assert.assertTrue(CollectionUtils.isEqualCollection(userProjectPermissions, read));
    }

    @Test
    public void _021revokeProjectPermissions2() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.revokeProjectPermissions(createSecurityContext(token), userId, projectId, read);

        Set<PermissionRule> userProjectPermissions = projectService.getUserProjectPermissions(userId, projectId);
        Assert.assertTrue(userProjectPermissions.isEmpty());
    }

    @Test
    public void _022grantProjectPermissions3() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.grantProjectPermissions(createSecurityContext(token), userId, projectId, read);

        Set<PermissionRule> userProjectPermissions = projectService.getUserProjectPermissions(userId, projectId);
        Assert.assertTrue(CollectionUtils.isEqualCollection(userProjectPermissions, read));
    }

    @Test
    public void _023revokeProjectPermissions3() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ);
        projectService.revokeProjectPermissions(createSecurityContext(token), userId, projectId, read);

        Set<PermissionRule> userProjectPermissions = projectService.getUserProjectPermissions(userId, projectId);
        Assert.assertTrue(CollectionUtils.isEqualCollection(userProjectPermissions, EnumSet.of(PermissionRule.WRITE)));

        read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.revokeProjectPermissions(createSecurityContext(token), userId, projectId, read);
    }

    @Test
    public void _024grantGrantPermissions() {
        projectService.revokeGrantPermissions(createSecurityContext(token), userId, grantId, EnumSet.of(PermissionRule.READ, PermissionRule.WRITE));
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ);
        projectService.grantGrantPermissions(createSecurityContext(token), userId, grantId, read);

        Set<PermissionRule> permissions = projectService.getUserGrantPermissions(userId, grantId);
        Assert.assertTrue(CollectionUtils.isEqualCollection(read, permissions));
    }

    @Test
    public void _025getAllGrantsByUser() {
        Set<ApiGrant> apiGrants = projectService.getAllGrants(createSecurityContext(token), userId, projectId);
        assertTrue(apiGrants.size() > 0);
    }

    @Test
    public void _026getPermissionsByGrant() {
        Set<PermissionRule> rules = (Set<PermissionRule>) projectService.getUserGrantPermissions(userId, grantId);
        assertTrue(rules.size() > 0);
    }

    @Test
    public void _027loadUsersForRepo() {
        Set<LdapUser> projectUsers = projectService.getGrantUsers(createSecurityContext(token), grantId);
        boolean found = false;
        for (LdapUser u : projectUsers) {
            if (u.getGuid().equals(userId)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test(expected = NotFoundException.class)
    public void _028loadUsersForNotExistRepo() {
        Set<LdapUser> projectUsers = projectService.getGrantUsers(createSecurityContext(token), UUID.randomUUID());
        boolean found = false;
        for (LdapUser u : projectUsers) {
            if (u.getGuid().equals(userId)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void _029revokeGrantPermissions() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ);
        projectService.revokeGrantPermissions(createSecurityContext(token), userId, grantId, read);

        Set<PermissionRule> permissions = projectService.getUserGrantPermissions(userId, grantId);
        Assert.assertTrue(permissions.isEmpty());
    }

    @Test
    public void _030grantGrantPermissions2() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.grantGrantPermissions(createSecurityContext(token), userId, grantId, read);

        Set<PermissionRule> permissions = projectService.getUserGrantPermissions(userId, grantId);
        Assert.assertTrue(CollectionUtils.isEqualCollection(read, permissions));
    }

    @Test
    public void _031revokeGrantPermissions2() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.revokeGrantPermissions(createSecurityContext(token), userId, grantId, read);

        Set<PermissionRule> permissions = projectService.getUserGrantPermissions(userId, grantId);
        Assert.assertTrue(permissions.isEmpty());
    }

    @Test
    public void _032grantGrantPermissions3() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ);
        projectService.grantGrantPermissions(createSecurityContext(token), userId, grantId, read);

        Set<PermissionRule> permissions = projectService.getUserGrantPermissions(userId, grantId);
        Assert.assertTrue(CollectionUtils.isEqualCollection(read, permissions));
    }

    @Test
    public void _033revokeGrantPermissions3() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.revokeGrantPermissions(createSecurityContext(token), userId, grantId, read);

        Set<PermissionRule> permissions = projectService.getUserGrantPermissions(userId, grantId);
        Assert.assertTrue(permissions.isEmpty());
    }

    @Test(expected = NotFoundException.class)
    public void _034getPermissionsByNotExistsGrant() {
        projectService.getUserGrantPermissions(userId, UUID.randomUUID());
    }

    @Test(expected = NotFoundException.class)
    public void _035getGrantPermissionsByNotExistsUser() {
        projectService.getUserGrantPermissions(UUID.randomUUID(), grantId);
    }

    @Test
    public void _036grantProjectPermissionsWithCascade() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.grantProjectPermissions(createSecurityContext(token), userId, projectId, read);

        Set<PermissionRule> userProjectPermissions = projectService.getUserProjectPermissions(userId, projectId);
        Assert.assertTrue(CollectionUtils.isEqualCollection(userProjectPermissions, read));

        Set<ApiGrant> allGrants = projectService.getAllGrants(createSecurityContext(token), projectId);
        for (ApiGrant grant : allGrants) {
            Set<PermissionRule> grantPermissions = projectService.getUserGrantPermissions(userId, grant.getId());
            Assert.assertTrue(CollectionUtils.isEqualCollection(grantPermissions, read));
        }
    }

    @Test
    public void _037revokeProjectPermissionsWithCascade() {
        EnumSet<PermissionRule> read = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        projectService.revokeProjectPermissions(createSecurityContext(token), userId, projectId, read);

        Set<PermissionRule> userProjectPermissions = projectService.getUserProjectPermissions(userId, projectId);
        Assert.assertTrue(userProjectPermissions.isEmpty());

        Set<ApiGrant> allGrants = projectService.getAllGrants(createSecurityContext(token), projectId);
        for (ApiGrant grant : allGrants) {
            Set<PermissionRule> grantPermissions = projectService.getUserGrantPermissions(userId, grant.getId());
            Assert.assertTrue(grantPermissions.isEmpty());
        }
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

    public static ApiCreateProject createProject() {
        return createProject(TEST_NAME, TEST_NAME, TEST_NAME);
    }

    public static ApiCreateProject createProject(String name, String key, String description) {
        ApiCreateProject project = new ApiCreateProject();
        project.setKey(key);
        project.setName(name);
        project.setDescription(description);
        return project;
    }
}
