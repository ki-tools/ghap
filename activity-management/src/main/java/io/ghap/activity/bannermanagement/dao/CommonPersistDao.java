package io.ghap.activity.bannermanagement.dao;

import com.google.inject.persist.Transactional;
import io.ghap.activity.bannermanagement.domain.BaseDBEntity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


public interface CommonPersistDao {
    <T extends BaseDBEntity> T create(T t);

    <T extends BaseDBEntity> T read(Class<T> clazz, Serializable id);

    <T extends BaseDBEntity> List<T> readAll(Class<T> clazz);

    <T extends BaseDBEntity> T update(T t);

    <T extends BaseDBEntity> void delete(T t);


    @Transactional
    <T extends BaseDBEntity> List<T> executeQuery(Class<T> clazz, String queryString, Map<String, Object> params);

    void executePingQuery();

}
