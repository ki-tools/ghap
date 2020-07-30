package io.ghap.logevents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventItem extends HashMap {

    public String getClientip() {
        return (String)get("clientip");
    }

    public void setClientip(String clientip) {
        put("clientip", clientip);
    }

    public String getTimestamp() {
        Object timestamp = get("timestamp");
        if (timestamp != null && !(timestamp instanceof String)) {
            return timestamp.toString();
        }
        return (String) timestamp;
    }

    public void setTimestamp(String timestamp) {
        put("timestamp", timestamp);
    }

    public List<String> getRoles() {
        return (List<String>) get("roles");
    }

    public void setRoles(List<String> roles) {
        put("roles", roles);
    }

    public String getEmail() {
        return (String) get("email");
    }

    public void setEmail(String email) {
        put("email", email);
    }

    public String getRemoteip() {
        return (String) get("remoteip");
    }

    public void setRemoteip(String remoteip) {
        put("remoteip", remoteip);
    }

    public String getFileName() {
        return (String) get("fileName");
    }

    public void setFileName(String fileName) {
        put("fileName", fileName);
    }

    public String getFileType() {
        return (String) get("fileType");
    }

    public void setFileType(String fileType) {
        put("fileType", fileType);
    }

    public Long getFileSize() {
        Object val = get("fileSize");

        if(val == null){
            return null;
        }
        else if(val instanceof Double){
            return ((Double) val).longValue();
        }

        return (long)val;
    }

    public void setFileSize(Long fileSize) {
        put("fileSize", fileSize);
    }

    public void setFileSize(Double fileSize) {
        if( fileSize != null ) {
            put("fileSize", fileSize.longValue());
        }
    }

    public String getUserName() {
        return (String) get("username");
    }

    public void setUserName(String userName) {
        put("username", userName);
    }

    public Object getMatchWithGitMasterRepo() {
        return get("matchWithGitMasterRepo");
    }

    public void setMatchWithGitMasterRepo(Object matchWithGitMasterRepo) {
        put("matchWithGitMasterRepo", matchWithGitMasterRepo);
    }
}
