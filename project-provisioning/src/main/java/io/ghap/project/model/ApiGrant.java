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
public interface ApiGrant {

    UUID getId();

    void setId(UUID id);

    String getName();

    void setName(String name);

    String getCloneUrl();

    void setCloneUrl(String cloneUrl);

    Set<PermissionRule> getPermissions();

    void setPermissions(Set<PermissionRule> permissions);
}
