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

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(description = "Exposes LRUQueryCache statistics from default cache")
public interface QueryCacheStatsMBean {

    @ManagedAttribute(description = "Number of cache entries that have been generated and put in the cache")
    long getCacheCount();

    @ManagedAttribute(description = "How many times a cached DocIdSet has been found and returned")
    long getHitCount();

    @ManagedAttribute(description = "How many times query was not contained in the cache")
    long getMissCount();

    @ManagedAttribute(description = "Number of times that a query has been looked up in the cache")
    long getTotalCount();

    @ManagedAttribute(description = "Number of cache entries that have been removed from the cache")
    long getEvictionCount();

    @ManagedAttribute(description = "Number of DocIdSets which are currently stored in the cache")
    long getCacheSize();

    @ManagedAttribute(description = "Memory usage of cache object in bytes")
    long getRamBytesUsed();
}
