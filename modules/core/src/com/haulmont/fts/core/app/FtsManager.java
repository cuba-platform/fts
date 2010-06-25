/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 24.06.2010 18:18:38
 *
 * $Id$
 */
package com.haulmont.fts.core.app;

import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.FtsQueue;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.FtsConfig;
import com.haulmont.fts.core.sys.ConfigLoader;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ManagedBean(FtsManagerAPI.NAME)
public class FtsManager implements FtsManagerAPI, FtsManagerMBean {

    private static Log log = LogFactory.getLog(FtsManager.class);

    private volatile Map<String, Set<String>> entities;

    private volatile boolean processingQueue;

    private Map<String, Set<String>> getEntities() {
        if (entities == null) {
            synchronized (this) {
                if (entities == null) {
                    ConfigLoader loader = new ConfigLoader();
                    entities = loader.loadConfiguration();
                }
            }
        }
        return entities;
    }

    public boolean isSearchable(BaseEntity entity) {
        Set<String> properties = getEntities().get(entity.getClass().getName());
        if (properties == null)
            return false;

        Set<String> dirty = PersistenceProvider.getDirtyFields(entity);
        for (String s : dirty) {
            if (properties.contains(s))
                return true;
        }
        return false;
    }

    public void processQueue() {
        log.debug("Start processing queue");
        if (processingQueue) {
            log.warn("Previous processing have not finished yet, exiting");
            return;
        }

        processingQueue = true;
        try {
            int maxSize = ConfigProvider.getConfig(FtsConfig.class).getIndexingBatchSize();
            List<FtsQueue> list;

            Transaction tx = Locator.createTransaction();
            try {
                EntityManager em = PersistenceProvider.getEntityManager();
                Query query = em.createQuery("select q from core$FtsQueue q order by q.createTs");
                query.setMaxResults(maxSize);
                list = query.getResultList();
                tx.commit();
            } finally {
                tx.end();
            }

            for (FtsQueue ftsQueue : list) {
                processQueueItem(ftsQueue.getEntityName(), ftsQueue.getEntityId());

                tx = Locator.createTransaction();
                try {
                    EntityManager em = PersistenceProvider.getEntityManager();
                    Query query = em.createQuery("delete from core$FtsQueue q where q.id = ?1");
                    query.setParameter(1, ftsQueue.getId());
                    query.executeUpdate();
                    tx.commit();
                } finally {
                    tx.end();
                }
            }
            log.debug(list.size() + " queue items succesfully processed");
        } finally {
            processingQueue = false;
        }
    }

    public String processQueueJmx() {
        try {
            processQueue();
            return "Done";
        } catch (Exception e) {
            return ExceptionUtils.getStackTrace(e);
        }
    }

    private void processQueueItem(String entityName, UUID entityId) {

    }
}
