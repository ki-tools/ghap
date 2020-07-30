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
public class ApiCreateProject implements ApiProject {

    private UUID id;

    @NotBlank
    @Length(min = 1, max = 255)
    private String name;

    @NotBlank
    @Length(min = 1, max = 255)
    private String key;

    @Length(min = 1, max = 255)
    private String description;

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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
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
