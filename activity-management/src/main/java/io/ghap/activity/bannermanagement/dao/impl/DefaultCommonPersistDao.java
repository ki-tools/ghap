package io.ghap.activity.bannermanagement.dao.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import io.ghap.activity.bannermanagement.dao.CommonPersistDao;
import io.ghap.activity.bannermanagement.domain.BaseDBEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;
import java.util.Map;


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
}
