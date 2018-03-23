/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import org.apache.lucene.search.IndexSearcher;
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
}
