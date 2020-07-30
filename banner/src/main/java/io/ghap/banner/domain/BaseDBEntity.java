package io.ghap.banner.domain;

import java.io.Serializable;
import java.util.UUID;


public interface BaseDBEntity extends Serializable {

    UUID getId();

    void setId(UUID id);

}

