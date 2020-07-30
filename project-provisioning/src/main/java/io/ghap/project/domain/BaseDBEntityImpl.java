package io.ghap.project.domain;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

/**
 */
@MappedSuperclass
public class BaseDBEntityImpl implements BaseDBEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "EXTERNAL_ID", unique = true)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseDBEntityImpl that = (BaseDBEntityImpl) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return !(externalId != null ? !externalId.equals(that.externalId) : that.externalId != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (externalId != null ? externalId.hashCode() : 0);
        return result;
    }
}
