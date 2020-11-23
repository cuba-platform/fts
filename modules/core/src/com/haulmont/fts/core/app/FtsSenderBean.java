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
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.FtsSender;
import com.haulmont.cuba.core.app.ServerInfoAPI;
import com.haulmont.cuba.core.entity.*;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.fts.core.jmx.FtsManagerMBean;
import com.haulmont.fts.core.sys.EntityDescr;
import com.haulmont.fts.core.sys.EntityDescrsManager;
import com.haulmont.fts.global.FtsConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

@Component(FtsSender.NAME)
public class FtsSenderBean implements FtsSender {

    protected FtsManagerAPI manager;

    @Inject
    protected Metadata metadata;

    @Inject
    protected Persistence persistence;

    @Inject
    private FtsConfig coreConfig;

    private String serverId;

    @Inject
    protected EntityDescrsManager entityDescrsManager;

    @Inject
    public void setManager(FtsManagerAPI manager) {
        this.manager = manager;
    }

    @Inject
    public void setServerInfo(ServerInfoAPI serverInfo) {
        serverId = serverInfo.getServerId();
    }

    @Override
    public void enqueue(Entity entity, FtsChangeType changeType) {
        if (!manager.isEntityCanBeIndexed(entity.getMetaClass())) return;

        if (changeType.equals(FtsChangeType.DELETE)) {
            EntityDescr descr = entityDescrsManager.getDescrByMetaClass(entity.getMetaClass());
            if (descr != null) {
                enqueue(entity.getMetaClass().getName(), entity.getId(), FtsChangeType.DELETE);
            }
        }

        List<Entity> list = manager.getSearchableEntities(entity);
        if (!list.isEmpty()) {
            for (Entity e : list) {
                MetaClass metaClass = metadata.getSession().getClassNN(e.getClass());
                if (PersistenceHelper.isNew(e) && e instanceof BaseDbGeneratedIdEntity) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            Object entityId = e.getId();
                            try (Transaction tx = persistence.createTransaction()) {
                                enqueue(metaClass.getName(), entityId, FtsChangeType.UPDATE);
                                tx.commit();
                            }
                        }
                    });
                } else {
                    Object entityId = e.getId();
                    if (metadata.getTools().hasCompositePrimaryKey(metaClass) && HasUuid.class.isAssignableFrom(metaClass.getJavaClass())) {
                        entityId = ((HasUuid) e).getUuid();
                    }
                    enqueue(metaClass.getName(), entityId, FtsChangeType.UPDATE);
                }
            }
        }
    }

    @Override
    public void enqueue(String entityName, Object entityId, FtsChangeType changeType) {
        if (!manager.isEntityCanBeIndexed(metadata.getClassNN(entityName))) return;

        // Join to an existing transaction in main DB or create a new one if we came here with a tx for an additional DB
        try (Transaction tx = persistence.getTransaction()) {
            List<String> indexingHosts = coreConfig.getIndexingHosts();
            if (indexingHosts.isEmpty()) {
                persistQueueItem(entityName, entityId, changeType, null);
            } else {
                for (String indexingHost : indexingHosts) {
                    persistQueueItem(entityName, entityId, changeType, indexingHost);
                }
            }
            tx.commit();
        }
    }

    @Override
    public void enqueueFake(String entityName, Object entityId) {
        FtsQueue q = metadata.create(FtsQueue.class);
        q.setObjectEntityId(entityId);
        q.setEntityName(entityName);
        q.setFake(true);
        persistence.getEntityManager().persist(q);
    }

    protected void persistQueueItem(String entityName, Object entityId, FtsChangeType changeType,
                                    @Nullable String indexingHost) {
        FtsQueue q = metadata.create(FtsQueue.class);
        q.setObjectEntityId(entityId);
        q.setEntityName(entityName);
        q.setChangeType(changeType);
        q.setSourceHost(serverId);
        q.setIndexingHost(indexingHost);
        persistence.getEntityManager().persist(q);
    }

    @Override
    public void emptyQueue(String entityName) {
        EntityManager em = persistence.getEntityManager();
        Query q = em.createQuery("delete from sys$FtsQueue q where q.entityName = ?1");
        q.setParameter(1, entityName);
        q.executeUpdate();
    }

    @Override
    public void emptyFakeQueue(String entityName) {
        EntityManager em = persistence.getEntityManager();
        Query q = em.createQuery("delete from sys$FtsQueue q where q.entityName = ?1 and q.fake = true");
        q.setParameter(1, entityName);
        q.executeUpdate();
    }

    @Override
    public void emptyQueue() {
        EntityManager em = persistence.getEntityManager();
        Query q = em.createQuery("delete from sys$FtsQueue q");
        q.executeUpdate();
    }

    @Override
    public void initDefault() {
        FtsManagerMBean ftsMBean = (FtsManagerMBean) manager;
        ftsMBean.setEnabled(true);
        ftsMBean.reindexAll();
        ftsMBean.processQueue();
    }
}