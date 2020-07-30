package io.ghap.activity.bannermanagement.domain;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

/**
 */
@MappedSuperclass
public class BaseDBEntityImpl implements BaseDBEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseDBEntityImpl)) return false;

        BaseDBEntityImpl that = (BaseDBEntityImpl) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
