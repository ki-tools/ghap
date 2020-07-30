package io.ghap.project.dao.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.UnitOfWork;
import com.netflix.config.ConfigurationManager;
import io.ghap.project.dao.CleanupDao;
import io.ghap.project.dao.CommonPersistDao;
import io.ghap.project.dao.UserManagementDao;
import io.ghap.project.domain.ObjectClass;
import io.ghap.project.domain.Permission;
import io.ghap.project.domain.User;
import io.ghap.project.model.LdapGroup;
import io.ghap.project.model.LdapUser;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 */
@Singleton
public class DefaultCleanupDao implements CleanupDao {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private CommonPersistDao commonPersistDao;

    @Inject
    private UserManagementDao userManagementDao;

    @Inject
    private UnitOfWork unitOfWork;

    private static final Timer timer = new Timer();

    @Override
    public int cleanupUsers(String accessToken) {
        Set<LdapUser> allUsers = userManagementDao.getAllUsers(accessToken);
        List<String> guids = allUsers.stream().map(e -> e.getGuid().toString()).collect(Collectors.toList());
        int result = removePermissions(guids, ObjectClass.user);
        return result;
    }

    @Override
    public int cleanupGroups(String accessToken) {
        Set<LdapGroup> allGroups = userManagementDao.getAllGroups(accessToken);
        List<String> guids = allGroups.stream().map(e -> e.getGuid().toString()).collect(Collectors.toList());
        int result = removePermissions(guids, ObjectClass.group);
        return result;
    }

    @Override
    public void runCleanupProcedure() {
        log.info("start run cleanup procedure");
        log.info("start log in");
        String accessToken;
        try {
            disableSSLCertificateChecking();
            accessToken = login();
        } catch (Exception e) {
            log.error("error during login", e);
            return;
        }
        log.info("successfully login with access token = " + accessToken);

        try {
            int i = cleanupUsers(accessToken);
            log.info("deleted {} permissions and users", i);
        } catch (Exception e) {
            log.error("error cleanup users ", e);
        }

        try {
            int i = cleanupGroups(accessToken);
            log.info("deleted {} permissions and groups", i);
        } catch (Exception e) {
            log.error("error cleanup groups", e);
        }
    }

    @Override
    public void scheduleCleanupProcedure() {
        int delay = (3600 + new Random().nextInt(3600)) * 1000;
        log.info("next task delay = {}", delay);
        timer.schedule(new Task(), delay);
    }

    private int removePermissions(List<String> guids, ObjectClass objectClass) {
        unitOfWork.begin();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("list", guids);
            params.put("objectClass", objectClass);
            int result = commonPersistDao.executeUpdate(
                    "delete from Permission e where e.user in (select u from User u where u.externalId not in :list and u.objectClass = :objectClass)",
                    params);
            result += commonPersistDao.executeUpdate(
                    "delete from User e where e.externalId  not in :list and e.objectClass = :objectClass",
                    params);
            return result;
        } finally {
            unitOfWork.end();
        }
    }

    public static String login() throws IOException, URISyntaxException {
        AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
        String oauthUrl = configInstance.getString("oauth.admin.url");
        String username = configInstance.getString("oauth.login");
        String password = configInstance.getString("oauth.password");
        String sessionId = getSessionId();

        URL url = new URL(oauthUrl.substring(0, oauthUrl.lastIndexOf("/oauth") + 1) + "j_spring_security_check");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestProperty("Cookie", "JSESSIONID=" + sessionId);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        List<NameValuePair> list = new ArrayList<>(3);
        list.add(new BasicNameValuePair("j_username", username));
        list.add(new BasicNameValuePair("j_password", password));
        list.add(new BasicNameValuePair("user-policy", "on"));
        new UrlEncodedFormEntity(list).writeTo(conn.getOutputStream());

        int responseCode = conn.getResponseCode();
        List<String> headers = conn.getHeaderFields().get("Set-Cookie");
        if (headers != null) {
            for (String header : headers) {
                sessionId = header;
                if (!sessionId.contains("JSESSIONID")) {
                    continue;
                }
                sessionId = sessionId.substring(sessionId.indexOf("=") + 1, sessionId.indexOf(";"));
                break;
            }
        }
        String location = conn.getHeaderField("location");
        conn.disconnect();

        url = new URL(location);
        conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestProperty("Cookie", "ppolicyread=read; JSESSIONID=" + sessionId);

        responseCode = conn.getResponseCode();
        String fragment = new URIBuilder(conn.getHeaderField("location")).getFragment();
        conn.disconnect();
        int access_token = fragment.indexOf("access_token") + "access_token".length() + 1;
        String token = fragment.substring(access_token, fragment.indexOf("&", access_token));
        return token;
    }

    public static String getSessionId() throws IOException {
        AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
        String oauthUrl = configInstance.getString("oauth.admin.url");
        URL url = new URL(oauthUrl + "/authorize?client_id=projectservice&response_type=token&redirect_uri=http://www.google.com");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        String sessionId = conn.getHeaderField("Set-Cookie");
        sessionId = sessionId.substring(sessionId.indexOf("=") + 1, sessionId.indexOf(";"));
        conn.disconnect();
        return sessionId;
    }

    private static void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private class Task extends TimerTask {
        @Override
        public void run() {
            try {
                runCleanupProcedure();
            } finally {
                scheduleCleanupProcedure();
            }
        }
    }
}
