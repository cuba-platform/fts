/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
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
import com.haulmont.cuba.core.sys.PersistenceImpl;
import com.haulmont.fts.core.jmx.FtsManagerMBean;
import com.haulmont.fts.global.FtsConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
        List<Entity> list = manager.getSearchableEntities(entity);
        if (!list.isEmpty()) {
            if (changeType.equals(FtsChangeType.DELETE)) {
                MetaClass metaClass = metadata.getSession().getClassNN(entity.getClass());
                enqueue(metaClass.getName(), entity.getId(), FtsChangeType.DELETE);
            }

            for (Entity e : list) {
                MetaClass metaClass = metadata.getSession().getClassNN(e.getClass());
                Object entityId = e.getId();
                if (PersistenceHelper.isNew(e) && e instanceof BaseDbGeneratedIdEntity) {
                    String storeName = metadata.getTools().getStoreName(metaClass);
                    List<Consumer<Integer>> runAfterCompletion = persistence.getEntityManagerContext(storeName).getAttribute(PersistenceImpl.RUN_AFTER_COMPLETION_ATTR);
                    if (runAfterCompletion == null) {
                        runAfterCompletion = new ArrayList<>();
                        persistence.getEntityManagerContext(storeName).setAttribute(PersistenceImpl.RUN_AFTER_COMPLETION_ATTR, runAfterCompletion);
                    }
                    Object finalEntityId = entityId;
                    runAfterCompletion.add((txStatus) -> {
                        if (TransactionSynchronization.STATUS_COMMITTED == txStatus)
                            enqueue(metaClass.getName(), finalEntityId, FtsChangeType.UPDATE);
                    });
                } else {
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