/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Stores;
import com.haulmont.fts.global.FtsConfig;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@Component(EntitiesCollector.NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EntitiesCollector {

    public static final String NAME = "fts_EntitiesCollector";
    protected static final int LOADING_SIZE = 500;
    protected MetaClass metaClass;
    protected String storeName;
    protected boolean idOnly;
    protected boolean excludeFromQueue;
    @Inject
    protected Persistence persistence;
    @Inject
    protected Metadata metadata;
    @Inject
    protected FtsConfig ftsConfig;

    public EntitiesCollector(MetaClass metaClass) {
        this.metaClass = metaClass;
    }

    @PostConstruct
    protected void init() {
        this.storeName = metadata.getTools().getStoreName(metaClass);
    }

    public void setIdOnly(boolean idOnly) {
        this.idOnly = idOnly;
    }

    public void setExcludeFromQueue(boolean excludeInQueue) {
        this.excludeFromQueue = excludeInQueue;
    }

    @SuppressWarnings("unchecked")
    public List loadResults() {
        if (!Stores.isMain(storeName)) {
            EntityManager storeEm = persistence.getEntityManager(storeName);
            if (excludeFromQueue) {
                EntityManager mainEm = persistence.getEntityManager();
                List ids = mainEm.createQuery(getExcludeIdsQueryString())
                        .setParameter("entityName", metaClass.getName())
                        .getResultList();
                if (ids.isEmpty()) {
                    return storeEm.createQuery(getQueryString())
                            .setParameter("ids", ids)
                            .getResultList();
                }
                List result = new ArrayList();
                for (int i = 0; i < ids.size(); i += LOADING_SIZE) {
                    List loadingIds = ids.subList(i, Math.min(i + LOADING_SIZE, ids.size()));
                    List loadingResult = storeEm.createQuery(getQueryString())
                            .setParameter("ids", loadingIds)
                            .setMaxResults(ftsConfig.getReindexBatchSize())
                            .getResultList();
                    result.addAll(loadingResult);
                    if (result.size() == ftsConfig.getReindexBatchSize()) {
                        break;
                    }
                }
                return result;
            } else {
                return storeEm.createQuery(getQueryString())
                        .getResultList();
            }
        } else {
            EntityManager em = persistence.getEntityManager();
            Query query = em.createQuery(getQueryString());
            if (excludeFromQueue) {
                query.setParameter("entityName", metaClass.getName());
                query.setMaxResults(ftsConfig.getReindexBatchSize());
            }
            return query.getResultList();
        }
    }


    protected String getQueryString() {
        StringBuilder query = new StringBuilder();
        if (idOnly) {
            query.append(format("select e.%s from %s e", metadata.getTools().getPrimaryKeyName(metaClass), metaClass.getName()));
        } else {
            query.append(format("select e from %s e", metaClass.getName()));
        }
        if (excludeFromQueue) {
            query.append(" where ");
            if (Stores.isMain(storeName)) {
                query.append(format("e.%s not in (%s)",
                        metadata.getTools().getPrimaryKeyName(metaClass),
                        getExcludeIdsQueryString()));
            } else {
                query.append(format("e.%s not in (:ids)",
                        metadata.getTools().getPrimaryKeyName(metaClass)));
            }
        }
        return query.toString();
    }

    protected String getExcludeIdsQueryString() {
        return format("select q.%s from sys$FtsQueue q where q.entityName = :entityName", getQueueIdPropertyName());
    }

    protected String getQueueIdPropertyName() {
        MetaProperty primaryKey = metadata.getTools().getPrimaryKeyProperty(metaClass);
        if (primaryKey != null) {
            Class type = primaryKey.getJavaType();
            if (UUID.class.equals(type)) {
                return "entityId";
            } else if (Long.class.equals(type)) {
                return "longEntityId";
            } else if (Integer.class.equals(type)) {
                return "intEntityId";
            } else if (String.class.equals(type)) {
                return "stringEntityId";
            } else {
                throw new IllegalStateException(
                        String.format("Unsupported primary key type: %s for %s", type.getSimpleName(), metaClass.getName()));
            }
        } else {
            throw new IllegalStateException(
                    String.format("Primary key not found for %s", metaClass.getName()));
        }
    }
}
