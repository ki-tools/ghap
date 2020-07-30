package io.ghap.project.dao;

import com.google.inject.persist.Transactional;
import io.ghap.project.domain.BaseDBEntity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface CommonPersistDao {
    <T extends BaseDBEntity> T create(T t);

    <T extends BaseDBEntity> T read(Class<T> clazz, Serializable id);

    <T extends BaseDBEntity> List<T> readAll(Class<T> clazz);

    <T extends BaseDBEntity> T update(T t);

    <T extends BaseDBEntity> void delete(T t);

    <T extends BaseDBEntity> T getByExternalId(Class<T> clazz, String externalId);

    @Transactional
    <T extends BaseDBEntity> T getByExternalIdCI(Class<T> clazz, String externalId);

    <T extends BaseDBEntity> List<T> getByParentId(Class<T> clazz, Serializable parentId);

    <T extends BaseDBEntity> List<T> getByParentId(Class<T> clazz, Serializable parentId, String parentFieldName);

    @Transactional
    <T extends BaseDBEntity> List<T> executeQuery(Class<T> clazz, String queryString, Map<String, Object> params);

    void executePingQuery();

    @Transactional
    <T extends BaseDBEntity> List<T> getByExternalId(Class<T> clazz, Set<String> ids);

    @Transactional
    <T extends BaseDBEntity> List<T> getByExternalIdCI(Class<T> clazz, Set<String> ids);

    @Transactional
    <T extends BaseDBEntity> int executeUpdate(String queryString, Map<String, Object> params);
}
