/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
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
