package io.ghap.project.model;

import io.ghap.project.domain.PermissionRule;

import java.util.Set;
import java.util.UUID;

/**
 */
public interface ApiProject {
    UUID getId();

    void setId(UUID id);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    String getKey();

    void setKey(String key);

    Set<PermissionRule> getPermissions();

    void setPermissions(Set<PermissionRule> permissions);
}
