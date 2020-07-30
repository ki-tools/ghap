package io.ghap.userdata.logevents;

import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;

@Singleton
public class TestEventlogger implements EventLogger {

    @Override
    public void sendDownloadEvent(String keyName, long contentLength, String contentType, String authHeader, String remoteAddr, SecurityContext securityContext) throws IOException {
        //ignore
    }
}
