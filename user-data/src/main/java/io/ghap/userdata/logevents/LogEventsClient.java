package io.ghap.userdata.logevents;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import io.ghap.logevents.*;
import io.ghap.oauth.OAuthUser;
import io.ghap.oauth.OauthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.ghap.logevents.DestinationType.ELASTICSEARCH;
import static io.ghap.logevents.Indexes.GHAP;

@Singleton
public class LogEventsClient implements EventLogger {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private EventsClient eventsClient;

    @Configuration("logevents.url")
    private String logeventsUrl;

    @PostConstruct
    public void init(){
        Map conf = new HashMap<>();
        conf.put("uri", logeventsUrl);
        eventsClient = EventsClientFactory.build(ELASTICSEARCH, GHAP, conf);
    }

    @PreDestroy
    public void destroy(){
        try {
            eventsClient.close();
        } catch (Exception e) {
            closeQuietly(eventsClient, log);
        }
    }

    public void sendDownloadEvent(String keyName, long contentLength, String contentType, boolean matchWithGitMasterRepo, String authHeader, String remoteAddr, SecurityContext securityContext) throws IOException {
        //OAuthPrincipal user = (OAuthPrincipal)securityContext.getUserPrincipal();
        OAuthUser user = OauthUtils.getOAuthUser(securityContext);
        String username = securityContext.getUserPrincipal().getName();

        Map data = new HashMap<>();
        data.put("username", username);
        data.put("email", user == null ? null:user.getEmail());
        data.put("roles", user == null ? null: new ArrayList<>(user.getRoles()));
        data.put("remoteip", remoteAddr);
        data.put("fileName", keyName);
        data.put("fileType", contentType);
        data.put("fileSize", contentLength);

        //projectProvisioningClient.exists(keyName, authHeader, securityContext)
        data.put("matchWithGitMasterRepo", matchWithGitMasterRepo);
        eventsClient.sendAsync(Types.USER_WORKSPACE_DOWNLOAD, data, new ResultHandler<JsonObject>(){

            @Override
            public void completed(JsonObject result) {
                log.debug("Event about \"" + keyName + "\" download by " + username + " was sent");
            }

            @Override
            public void failed(Exception ex) {
                log.error("Cannot log \"download\" event(about \"" + keyName + "\" for user " + username + ", IP " + getLocalIP() + ") due to the following error.", ex);
            }
        });
    }

    /**
     * Closes the given Closeable quietly.
     * @param is the given closeable
     * @param logger logger used to log any failure should the close fail
     */
    public static void closeQuietly(Closeable is, Logger logger) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                if (logger.isDebugEnabled())
                    logger.debug("Ignore failure in closing the Closeable", ex);
            }
        }
    }

    private String getLocalIP(){
        try {
            return InetUtils.getLocalIP(false);
        } catch (SocketException e) {
            return "undefined";
        }
    }
}
