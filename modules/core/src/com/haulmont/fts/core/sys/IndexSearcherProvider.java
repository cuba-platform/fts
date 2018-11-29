/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;

/**
 * The class is used for obtaining instances of {@link IndexSearcher} and {@link SearcherManager}.
 */
public interface IndexSearcherProvider {

    String NAME = "fts_IndexSearcherProvider";

    /**
     * Returns an instance of the {@link SearcherManager}. Application uses the single instance of this class.
     */
    SearcherManager getSearcherManager();

    /**
     * Returns an instance of the {@link IndexSearcher}. After search operations are completed, the {@link
     * #releaseIndexSearcher(IndexSearcher)} must be invoked.
     */
    IndexSearcher acquireIndexSearcher();

    /**
     * The method must be invoked for {@link IndexSearcher} got with the {@link #acquireIndexSearcher()} after all
     * search operations are completed
     */
    void releaseIndexSearcher(IndexSearcher indexSearcher);

    /**
     * Returns static DefaultQueryCache from {@link IndexSearcher}.
     */
    LRUQueryCache getDefaultQueryCache();
}
