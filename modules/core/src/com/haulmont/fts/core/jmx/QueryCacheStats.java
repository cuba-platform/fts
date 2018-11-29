/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.jmx;

import com.haulmont.fts.core.sys.IndexSearcherProvider;
import org.apache.lucene.search.LRUQueryCache;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component("fts_QueryCacheStatsMBean")
public class QueryCacheStats implements QueryCacheStatsMBean {
    @Inject
    protected IndexSearcherProvider indexSearcherProvider;

    @Override
    public long getCacheCount() {
        LRUQueryCache defaultQueryCache = indexSearcherProvider.getDefaultQueryCache();
        return defaultQueryCache.getCacheCount();
    }

    @Override
    public long getHitCount() {
        LRUQueryCache defaultQueryCache = indexSearcherProvider.getDefaultQueryCache();
        return defaultQueryCache.getHitCount();
    }

    @Override
    public long getMissCount() {
        LRUQueryCache defaultQueryCache = indexSearcherProvider.getDefaultQueryCache();
        return defaultQueryCache.getMissCount();
    }

    @Override
    public long getTotalCount() {
        LRUQueryCache defaultQueryCache = indexSearcherProvider.getDefaultQueryCache();
        return defaultQueryCache.getTotalCount();
    }

    @Override
    public long getEvictionCount() {
        LRUQueryCache defaultQueryCache = indexSearcherProvider.getDefaultQueryCache();
        return defaultQueryCache.getEvictionCount();
    }

    @Override
    public long getCacheSize() {
        LRUQueryCache defaultQueryCache = indexSearcherProvider.getDefaultQueryCache();
        return defaultQueryCache.getCacheSize();
    }

    @Override
    public long getRamBytesUsed() {
        LRUQueryCache defaultQueryCache = indexSearcherProvider.getDefaultQueryCache();
        return defaultQueryCache.ramBytesUsed();
    }
}
