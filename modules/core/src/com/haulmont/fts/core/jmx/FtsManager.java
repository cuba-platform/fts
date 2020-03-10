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

package com.haulmont.fts.core.jmx;

import com.google.common.base.Strings;
import com.haulmont.cuba.security.app.Authenticated;
import com.haulmont.fts.core.app.FtsManagerAPI;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Queue;

@Component("fts_FtsManagerMBean")
public class FtsManager implements FtsManagerMBean {

    private static final Logger log = LoggerFactory.getLogger(FtsManager.class);

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
        if (Strings.isNullOrEmpty(entityName)) return "Fill entity name";
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
        if (Strings.isNullOrEmpty(entityName)) return "Fill entity name";
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

    @Override
    public String reindexNextBatch() {
        try {
            manager.reindexNextBatch();
            return String.format("Reindex next batch");
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

    @Override
    public String getIndexFormatVersion() {
        return manager.getIndexFormatVersion();
    }

    @Override
    public String getLatestIndexFormatVersion() {
        return manager.getLatestIndexFormatVersion();
    }
}