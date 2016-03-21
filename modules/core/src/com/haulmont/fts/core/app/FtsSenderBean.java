/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.app.FtsSender;
import com.haulmont.cuba.core.app.ServerInfoAPI;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.entity.FtsQueue;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.fts.core.jmx.FtsManagerMBean;

import org.springframework.stereotype.Component;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/**
 */
@Component(FtsSender.NAME)
public class FtsSenderBean implements FtsSender {

    protected FtsManagerAPI manager;

    @Inject
    protected Metadata metadata;

    @Inject
    protected Persistence persistence;

    @Inject
    private FtsCoreConfig coreConfig;

    private String serverId;

    @Inject
    public void setManager(FtsManagerAPI manager) {
        this.manager = manager;
    }

    @Inject
    public void setServerInfo(ServerInfoAPI serverInfo) {
        serverId = serverInfo.getServerId();
    }

    public void enqueue(BaseEntity<UUID> entity, FtsChangeType changeType) {
        List<BaseEntity> list = manager.getSearchableEntities(entity);
        if (!list.isEmpty()) {
            if (changeType.equals(FtsChangeType.DELETE)) {
                MetaClass metaClass = metadata.getSession().getClassNN(entity.getClass());
                enqueue(metaClass.getName(), entity.getId(), FtsChangeType.DELETE);
            }

            for (BaseEntity e : list) {
                MetaClass metaClass = metadata.getSession().getClassNN(e.getClass());
                enqueue(metaClass.getName(), (UUID) e.getId(), FtsChangeType.UPDATE);
            }
        }
    }

    public void enqueue(String entityName, UUID entityId, FtsChangeType changeType) {
        List<String> indexingHosts = coreConfig.getIndexingHosts();
        if (indexingHosts.isEmpty()) {
            persistQueueItem(entityName, entityId, changeType, null);
        } else {
            for (String indexingHost : indexingHosts) {
                persistQueueItem(entityName, entityId, changeType, indexingHost);
            }
        }
    }

    @Override
    public void enqueueFake(String entityName, UUID entityId) {
        FtsQueue q = metadata.create(FtsQueue.class);
        q.setEntityId(entityId);
        q.setEntityName(entityName);
        q.setFake(true);
        persistence.getEntityManager().persist(q);
    }

    protected void persistQueueItem(String entityName, UUID entityId, FtsChangeType changeType,
                                    @Nullable String indexingHost) {
        FtsQueue q = metadata.create(FtsQueue.class);
        q.setEntityId(entityId);
        q.setEntityName(entityName);
        q.setChangeType(changeType);
        q.setSourceHost(serverId);
        q.setIndexingHost(indexingHost);
        persistence.getEntityManager().persist(q);
    }

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

    public void emptyQueue() {
        EntityManager em = persistence.getEntityManager();
        Query q = em.createQuery("delete from sys$FtsQueue q");
        q.executeUpdate();
    }

    public void initDefault() {
        FtsManagerMBean ftsMBean = (FtsManagerMBean) manager;
        ftsMBean.setEnabled(true);
        ftsMBean.reindexAll();
        ftsMBean.processQueue();
    }
}