package io.ghap.reporting.data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionMismatchItem {

    private String program;
    private String grant;
    private String group;
    private String user;
    private Boolean match;
    private Boolean existInStash;
    private Boolean existInDb;
    private String stashPermissions;
    private String dbPermissions;

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getGrant() {
        return grant;
    }

    public void setGrant(String grant) {
        this.grant = grant;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Boolean getMatch() {
        return match;
    }

    public void setMatch(Boolean match) {
        this.match = match;
    }

    public Boolean getExistInStash() {
        return existInStash;
    }

    public void setExistInStash(Boolean existInStash) {
        this.existInStash = existInStash;
    }

    public Boolean getExistInDb() {
        return existInDb;
    }

    public void setExistInDb(Boolean existInDb) {
        this.existInDb = existInDb;
    }

    public String getStashPermissions() {
        return stashPermissions;
    }

    public void setStashPermissions(String stashPermissions) {
        this.stashPermissions = stashPermissions;
    }

    public String getDbPermissions() {
        return dbPermissions;
    }

    public void setDbPermissions(String dbPermissions) {
        this.dbPermissions = dbPermissions;
    }
}
