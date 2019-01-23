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

package com.haulmont.fts.core.sys;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;

@Component(IndexSearcherProvider.NAME)
public class IndexSearcherProviderBean implements IndexSearcherProvider {

    protected volatile SearcherManager searcherManager;

    @Inject
    protected IndexWriterProvider indexWriterProvider;

    public SearcherManager getSearcherManager() {
        if (searcherManager == null) {
            synchronized (this) {
                if (searcherManager == null) {
                    try {
                        searcherManager = createSearcherManager();
                    } catch (IOException e) {
                        throw new RuntimeException("Error on creating SearcherManager", e);
                    }
                }
            }
        }
        return searcherManager;
    }

    public IndexSearcher acquireIndexSearcher() {
        SearcherManager searcherManager = getSearcherManager();
        IndexSearcher indexSearcher = null;
        try {
            indexSearcher = searcherManager.acquire();
        } catch (IOException e) {
            throw new RuntimeException("Error on acquiring an IndexSearcher", e);
        }
        return indexSearcher;
    }

    public void releaseIndexSearcher(IndexSearcher indexSearcher) {
        SearcherManager searcherManager = getSearcherManager();
        try {
            searcherManager.release(indexSearcher);
        } catch (IOException e) {
            throw new RuntimeException("Error on releasing an IndexSearcher", e);
        }
    }

    protected SearcherManager createSearcherManager() throws IOException {
        return new SearcherManager(indexWriterProvider.getIndexWriter(), new SearcherFactory());
    }

    public LRUQueryCache getDefaultQueryCache() {
        return (LRUQueryCache) IndexSearcher.getDefaultQueryCache();
    }
}
