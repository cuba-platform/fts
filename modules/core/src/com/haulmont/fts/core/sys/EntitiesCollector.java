/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.fts.core.sys;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Stores;
import com.haulmont.fts.core.app.FtsManagerAPI;
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
    @Inject
    protected FtsManagerAPI ftsManager;

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
    public List<Object> loadResults() {
        if (!ftsManager.isEntityCanBeIndexed(metaClass)) return new ArrayList();

        if (!Stores.isMain(storeName)) {
            EntityManager storeEm = persistence.getEntityManager(storeName);
            if (excludeFromQueue) {
                EntityManager mainEm = persistence.getEntityManager();
                List<Object> ids = mainEm.createQuery(getExcludeIdsQueryString())
                        .setParameter("entityName", metaClass.getName())
                        .getResultList();
                if (ids.isEmpty()) {
                    return storeEm.createQuery(getQueryString())
                            .setParameter("ids", ids)
                            .getResultList();
                }
                List<Object> result = new ArrayList<>();
                for (int i = 0; i < ids.size(); i += LOADING_SIZE) {
                    List<Object> loadingIds = ids.subList(i, Math.min(i + LOADING_SIZE, ids.size()));
                    List<Object> loadingResult = storeEm.createQuery(getQueryString())
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
        String primaryKeyName = ftsManager.getPrimaryKeyPropertyForFts(metaClass).getName();
        if (idOnly) {
            query.append(format("select e.%s from %s e", primaryKeyName, metaClass.getName()));
        } else {
            query.append(format("select e from %s e", metaClass.getName()));
        }
        if (excludeFromQueue) {
            if (Stores.isMain(storeName)) {
                query.append(getExcludeIdsJoinString(primaryKeyName));
                query.append(" where ");
                query.append("q.id is null");
            } else {
                query.append(" where ");
                query.append(format("e.%s not in (:ids)",
                        primaryKeyName));
            }
        }
        return query.toString();
    }

    protected String getExcludeIdsJoinString(String primaryKey) {
        return format(" left join sys$FtsQueue q on q.%s = e.%s and q.entityName = :entityName", getQueueIdPropertyName(), primaryKey);
    }

    protected String getExcludeIdsQueryString() {
        return format("select q.%s from sys$FtsQueue q where q.entityName = :entityName", getQueueIdPropertyName());
    }

    protected String getQueueIdPropertyName() {
        MetaProperty primaryKey = ftsManager.getPrimaryKeyPropertyForFts(metaClass);
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
