package io.ghap.project.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StashPermission {

    private String permission;
    private StashUser user;
    private StashGroup group;

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public StashUser getUser() {
        return user;
    }

    public void setUser(StashUser user) {
        this.user = user;
    }

    public StashGroup getGroup() {
        return group;
    }

    public void setGroup(StashGroup group) {
        this.group = group;
    }
}
