package io.ghap.userdata.logevents;

import javax.ws.rs.core.SecurityContext;
import java.io.IOException;

public interface EventLogger {
    void sendDownloadEvent(String keyName, long contentLength, String contentType, boolean matchWithGitMasterRepo, String authHeader, String remoteAddr, SecurityContext securityContext) throws IOException;
}
