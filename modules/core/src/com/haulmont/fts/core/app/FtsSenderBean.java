/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 24.06.2010 17:34:00
 *
 * $Id$
 */
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.PersistenceProvider;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.app.FtsSender;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.entity.FtsQueue;
import com.haulmont.cuba.core.global.MetadataProvider;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@ManagedBean(FtsSender.NAME)
public class FtsSenderBean implements FtsSender {

    private FtsManagerAPI manager;

    @Inject
    public void setManager(FtsManagerAPI manager) {
        this.manager = manager;
    }

    public void enqueue(BaseEntity<UUID> entity, FtsChangeType changeType) {
        if (changeType.equals(FtsChangeType.DELETE)) {
            MetaClass metaClass = MetadataProvider.getSession().getClass(entity.getClass());
            enqueue(metaClass.getName(), entity.getId(), FtsChangeType.DELETE);
        }

        List<BaseEntity> list = manager.getSearchableEntities(entity);

        for (BaseEntity<UUID> e : list) {
            MetaClass metaClass = MetadataProvider.getSession().getClass(e.getClass());
            enqueue(metaClass.getName(), e.getId(), FtsChangeType.UPDATE);
        }
    }

    public void enqueue(String entityName, UUID entityId, FtsChangeType changeType) {
        FtsQueue q = new FtsQueue();
        q.setEntityId(entityId);
        q.setEntityName(entityName);
        q.setChangeType(changeType);

        PersistenceProvider.getEntityManager().persist(q);
    }

    public void emptyQueue(String entityName) {
        EntityManager em = PersistenceProvider.getEntityManager();
        Query q = em.createQuery("delete from core$FtsQueue q where q.entityName = ?1");
        q.setParameter(1, entityName);
        q.executeUpdate();
    }

    public void emptyQueue() {
        EntityManager em = PersistenceProvider.getEntityManager();
        Query q = em.createQuery("delete from core$FtsQueue q");
        q.executeUpdate();
    }
}
