package io.ghap.project.dao.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import io.ghap.project.dao.CommonPersistDao;
import io.ghap.project.domain.BaseDBEntity;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class DefaultCommonPersistDao implements CommonPersistDao {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private Provider<EntityManager> entityManagerProvider;

    @Override
    @Transactional
    public <T extends BaseDBEntity> T create(T t) {
        entityManagerProvider.get().persist(t);
        return t;
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> T read(Class<T> clazz, Serializable id) {
        return entityManagerProvider.get().find(clazz, id);

    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> List<T> readAll(Class<T> clazz) {
        Query q = entityManagerProvider.get().createQuery("from " + clazz.getSimpleName() + " c");
        return q.getResultList();
    }

    @Override
    @Transactional(ignore = {RollbackException.class})
    public <T extends BaseDBEntity> T update(T t) {
        return entityManagerProvider.get().merge(t);
    }

    @Override
    @Transactional(ignore = {RollbackException.class})
    public <T extends BaseDBEntity> void delete(T t) {
        entityManagerProvider.get().remove(entityManagerProvider.get().contains(t) ? t : entityManagerProvider.get().merge(t));
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> T getByExternalId(Class<T> clazz, String externalId) {
        TypedQuery<T> query = entityManagerProvider.get().createQuery("from " + clazz.getSimpleName() + " c where c.externalId = :extId", clazz);
        query.setParameter("extId", externalId);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> T getByExternalIdCI(Class<T> clazz, String externalId) {
        TypedQuery<T> query = entityManagerProvider.get().createQuery("from " + clazz.getSimpleName() + " c where LOWER(c.externalId) = LOWER(:extId)", clazz);
        query.setParameter("extId", externalId);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> List<T> getByParentId(Class<T> clazz, Serializable parentId) {
        return getByParentIdWithoutTransact(clazz, parentId, null);
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> List<T> getByParentId(Class<T> clazz, Serializable parentId, String parentFieldName) {
        return getByParentIdWithoutTransact(clazz, parentId, parentFieldName);
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> List<T> executeQuery(Class<T> clazz, String queryString, Map<String, Object> params) {
        TypedQuery<T> query = entityManagerProvider.get().createQuery(queryString, clazz);
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                query.setParameter(e.getKey(), e.getValue());
            }
        }
        return query.getResultList();
    }

    @Override
    public void executePingQuery() {
        Query query = entityManagerProvider.get().createNativeQuery("select 1");
        query.getSingleResult();
    }

    private <T extends BaseDBEntity> List<T> getByParentIdWithoutTransact(Class<T> clazz, Serializable parentId, String parentFieldName) {
        if (parentFieldName == null) {
            parentFieldName = "parentId";
        }
        TypedQuery<T> query = entityManagerProvider.get().createQuery("from " + clazz.getSimpleName() + " c where c." + parentFieldName + " = :parentId", clazz);
        query.setParameter("parentId", parentId);
        return query.getResultList();
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> List<T> getByExternalId(Class<T> clazz, Set<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        TypedQuery<T> query = entityManagerProvider.get().createQuery("from " + clazz.getSimpleName() + " c where c.externalId in (:extIds)", clazz);
        query.setParameter("extIds", ids);
        return query.getResultList();
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> List<T> getByExternalIdCI(Class<T> clazz, Set<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        TypedQuery<T> query = entityManagerProvider.get().createQuery("from " + clazz.getSimpleName() + " c where LOWER(c.externalId) in (:extIds)", clazz);
        query.setParameter("extIds", ids);
        return query.getResultList();
    }

    @Override
    @Transactional
    public <T extends BaseDBEntity> int executeUpdate(String queryString, Map<String, Object> params) {
        Query query = entityManagerProvider.get().createQuery(queryString);
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                query.setParameter(e.getKey(), e.getValue());
            }
        }
        return query.executeUpdate();
    }
}
