/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
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
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.FtsConfig;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.fts.core.jmx.FtsManagerMBean;

import javax.annotation.ManagedBean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(FtsSender.NAME)
public class FtsSenderBean implements FtsSender {

    protected FtsManagerAPI manager;

    @Inject
    protected Metadata metadata;

    @Inject
    protected Persistence persistence;

    private FtsConfig ftsConfig;

    private String serverId;

    @Inject
    public void setManager(FtsManagerAPI manager) {
        this.manager = manager;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        ftsConfig = configuration.getConfig(FtsConfig.class);
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
        List<String> indexingHosts = ftsConfig.getIndexingHosts();
        if (indexingHosts.isEmpty()) {
            persistQueueItem(entityName, entityId, changeType, null);
        } else {
            for (String indexingHost : indexingHosts) {
                persistQueueItem(entityName, entityId, changeType, indexingHost);
            }
        }
    }

    protected void persistQueueItem(String entityName, UUID entityId, FtsChangeType changeType,
                                    @Nullable String indexingHost) {
        FtsQueue q = new FtsQueue();
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
