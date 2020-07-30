package io.ghap.project.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StashRepo {
    private Long id;
    private String name;
    private String slug;
    private String scmId = "git";
    private String state;
    private String statusMessage;
    private Boolean forkable = false;
    private String cloneUrl;
    private StashLinks links;

    @JsonIgnore
    private Set<PermissionRule> permissions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getScmId() {
        return scmId;
    }

    public void setScmId(String scmId) {
        this.scmId = scmId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Boolean getForkable() {
        return forkable;
    }

    public void setForkable(Boolean forkable) {
        this.forkable = forkable;
    }

    public Set<PermissionRule> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionRule> permissions) {
        this.permissions = permissions;
    }

    public String getCloneUrl() {
        if (cloneUrl != null) {
            return cloneUrl;
        }
        StashLinks links = getLinks();
        if (links == null) {
            return null;
        }
        List<StashLink> clone = links.getClone();
        if (CollectionUtils.isEmpty(clone)) {
            return null;
        }
        for (StashLink link : clone) {
            if ("http".equalsIgnoreCase(link.getName())) {
                return link.getHref();
            }
        }
        return null;
    }

    public void setCloneUrl(String cloneUrl) {
        this.cloneUrl = cloneUrl;
    }

    public StashLinks getLinks() {
        return links;
    }

    public void setLinks(StashLinks links) {
        this.links = links;
    }
}
