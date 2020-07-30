package io.ghap.project.domain;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Set;


@Entity
@Table(name = "PROJECT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project extends BaseDBEntityImpl {

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project", orphanRemoval = true, cascade = {CascadeType.REMOVE})
    @JsonIgnore
    private Set<Repo> repos;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project", cascade = {CascadeType.REMOVE})
    @JsonIgnore
    private Set<Permission> permissions;

    public Set<Repo> getRepos() {
        return repos;
    }

    public void setRepos(Set<Repo> repos) {
        this.repos = repos;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
