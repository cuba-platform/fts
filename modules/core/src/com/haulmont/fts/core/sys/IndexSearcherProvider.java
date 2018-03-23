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

/**
 * The class is used for obtaining instances of {@link IndexSearcher} and {@link SearcherManager}.
 */
@Component("fts_IndexSearcherProvider")
public class IndexSearcherProvider {

    protected volatile SearcherManager searcherManager;

    @Inject
    protected IndexWriterProvider indexWriterProvider;

    /**
     * Returns an instance of the {@link SearcherManager}. Application uses the single instance of this class.
     */
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

    /**
     * Returns an instance of the {@link IndexSearcher}. After search operations are completed, the {@link
     * #releaseIndexSearcher(IndexSearcher)} must be invoked.
     */
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

    /**
     * The method must be invoked for {@link IndexSearcher} got with the {@link #acquireIndexSearcher()} after all
     * search operations are completed
     */
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
