package io.ghap.project.domain;

import java.io.Serializable;
import java.util.UUID;


public interface BaseDBEntity extends Serializable {

    UUID getId();

    void setId(UUID id);

    String getExternalId();

    void setExternalId(String externalId);
}

