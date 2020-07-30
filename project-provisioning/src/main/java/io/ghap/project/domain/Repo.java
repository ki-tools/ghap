package io.ghap.project.domain;


import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "REPO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repo implements BaseDBEntity {

    @ManyToOne
    @JoinColumn(name = "PROJECT_ID")
    private Project project;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "repo", cascade = {CascadeType.REMOVE})
    @JsonIgnore
    private Set<Permission> permissions;

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "EXTERNAL_ID", unique = false)
    private String externalId;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    @Override
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Repo repo = (Repo) o;

        return !(id != null ? !id.equals(repo.id) : repo.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
