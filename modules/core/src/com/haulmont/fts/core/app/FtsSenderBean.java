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
import com.haulmont.cuba.core.app.FtsSender;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.entity.FtsQueue;
import com.haulmont.cuba.core.global.MetadataProvider;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.util.UUID;

@ManagedBean(FtsSender.NAME)
public class FtsSenderBean implements FtsSender {

    private FtsManagerAPI manager;

    @Inject
    public void setManager(FtsManagerAPI manager) {
        this.manager = manager;
    }

    public void enqueue(BaseEntity<UUID> entity, FtsChangeType changeType) {
        if (!manager.isSearchable(entity))
            return;

        FtsQueue q = new FtsQueue();
        q.setEntityId(entity.getId());
        MetaClass metaClass = MetadataProvider.getSession().getClass(entity.getClass());
        q.setEntityName(metaClass.getName());
        q.setChangeType(changeType);

        PersistenceProvider.getEntityManager().persist(q);
    }
}
