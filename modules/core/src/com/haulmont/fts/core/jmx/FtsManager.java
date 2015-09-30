/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.fts.core.jmx;

import com.haulmont.cuba.security.app.Authenticated;
import com.haulmont.fts.core.app.FtsManagerAPI;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Component;
import javax.inject.Inject;
import java.util.Queue;

/**
 * @author krivopustov
 * @version $Id$
 */
@Component("fts_FtsManagerMBean")
public class FtsManager implements FtsManagerMBean {

    protected Log log = LogFactory.getLog(getClass());

    @Inject
    protected FtsManagerAPI manager;

    @Override
    public boolean isEnabled() {
        return manager.isEnabled();
    }

    @Override
    public void setEnabled(boolean value) {
        manager.setEnabled(value);
    }

    @Override
    public boolean isWriting() {
        return manager.isWriting();
    }

    @Override
    public boolean isReindexing() {
        return manager.isReindexing();
    }

    @Override
    public Queue<String> getReindexEntitiesQueue() {
        return manager.getReindexEntitiesQueue();
    }

    @Override
    public String processQueue() {
        try {
            // login performed inside processQueue()
            int count = manager.processQueue();
            return String.format("Done %d items", count);
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    @Override
    public String optimize() {
        return manager.optimize();
    }

    @Override
    public String upgrade() {
        return manager.upgrade();
    }

    @Authenticated
    @Override
    public String reindexEntity(String entityName) {
        try {
            manager.deleteIndexForEntity(entityName);
            int count = manager.reindexEntity(entityName);

            return String.format("Enqueued %d items. Reindexing will be performed on next processQueue invocation.", count);
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    @Authenticated
    @Override
    public String reindexAll() {
        try {
            manager.deleteIndex();
            int count = manager.reindexAll();

            return String.format("Enqueued %d items. Reindexing will be performed on next processQueue invocation.", count);
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    @Authenticated
    @Override
    public String asyncReindexEntity(String entityName) {
        try {
            manager.deleteIndexForEntity(entityName);
            manager.asyncReindexEntity(entityName);
            return String.format("Entity %s is marked for reindex. Entity instances will be added to the fts queue " +
                    "by reindexNextBatch method.", entityName);
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }


    @Authenticated
    @Override
    public String asyncReindexAll() {
        try {
            manager.deleteIndex();
            manager.asyncReindexAll();
            return "All entities are marked for reindexing. Entity instances will be added to the fts queue " +
                    "by reindexNextBatch method invocations.";
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    @Override
    public String processEntireQueue() {
        try {
            Integer total = 0;
            Integer count = null;
            while (count == null || count > 0) {
                count = manager.processQueue();
                total += count;
            }
            return String.format("Done %d items", total);
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }
}
