/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.fts.core.jmx;

import com.haulmont.cuba.security.app.Authenticated;
import com.haulmont.fts.core.app.FtsManagerAPI;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean("fts_FtsManagerMBean")
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
    public String upgradeIndexes() {
        return manager.upgradeIndexes();
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
}
