package io.ghap.project.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Set;

/**
 * @author MaximTulupov@certara.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StashProject {

    private String key;
    private String id;
    private String name;
    private String description;
    private Boolean isPublic = true;
    private String type;

    @JsonIgnore
    private Set<PermissionRule> permissions;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<PermissionRule> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionRule> permissions) {
        this.permissions = permissions;
    }
}
