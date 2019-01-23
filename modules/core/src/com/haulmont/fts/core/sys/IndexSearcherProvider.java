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
