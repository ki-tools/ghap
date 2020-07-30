package io.ghap.project.model;

import io.ghap.project.domain.PermissionRule;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiCreateGrant implements ApiGrant {
    private UUID id;

    @NotBlank
    @Length(min = 1, max = 255)
    private String name;

    private String cloneUrl;

    private Set<PermissionRule> permissions;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCloneUrl() {
        return cloneUrl;
    }

    @Override
    public void setCloneUrl(String cloneUrl) {
        this.cloneUrl= cloneUrl;
    }

    @Override
    public Set<PermissionRule> getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(Set<PermissionRule> permissions) {
        this.permissions = permissions;
    }
}
