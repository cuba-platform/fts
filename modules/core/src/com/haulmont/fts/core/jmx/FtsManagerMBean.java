/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.jmx;

import com.haulmont.cuba.core.sys.jmx.JmxRunAsync;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

import java.util.Queue;

public interface FtsManagerMBean {

    boolean isEnabled();

    void setEnabled(boolean value);

    boolean isWriting();

    boolean isReindexing();

    Queue<String> getReindexEntitiesQueue();

    @JmxRunAsync
    String processQueue();

    @JmxRunAsync
    String optimize();

    @JmxRunAsync
    String upgrade();

    @JmxRunAsync
    @ManagedOperation(description = "Reindex the given entity synchronously")
    @ManagedOperationParameters({@ManagedOperationParameter(name = "entityName", description = "")})
    String reindexEntity(String entityName);

    @JmxRunAsync
    @ManagedOperation(description = "Reindex all entities synchronously")
    String reindexAll();

    @ManagedOperation(description = "Reindex the given entity asynchronously. Entity instances will be added to the queue " +
            "in batches by the invocation of reindexNextBatch method from a scheduled task")
    @ManagedOperationParameters({@ManagedOperationParameter(name = "entityName", description = "")})
    String asyncReindexEntity(String entityName);

    @ManagedOperation(description = "Reindex all entities asynchronously. Entity instances will be added to the queue " +
            "in batches by the invocation of reindexNextBatch method from a scheduled task")
    String asyncReindexAll();

    @JmxRunAsync
    String processEntireQueue();

    @JmxRunAsync
    @ManagedOperation(description = "Reindex next entity from queue")
    String reindexNextBatch();
}
