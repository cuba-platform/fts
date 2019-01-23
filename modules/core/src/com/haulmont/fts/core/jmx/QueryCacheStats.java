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
