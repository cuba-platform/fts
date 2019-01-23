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

    @ManagedOperation(description = "Returns current index format version")
    String getIndexFormatVersion();

    @ManagedOperation(description = "Returns last index format version supported by lucene")
    String getLatestIndexFormatVersion();
}
