/*
 * Copyright (c) 2008-2017 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import java.util.Collection;
import java.util.List;

/**
 * INTERNAL.
 * Used by {@code FtsService} to search with Lucene.
 */
public interface LuceneSearcherAPI {
    String NAME = "fts_LuceneSearcher";

    /**
     * Check whether any new changes have occurred to the index since this searcher was created.
     */
    boolean isCurrent();

    List<EntityInfo> searchAllField(String searchTerm, int maxResults);

    List<EntityInfo> searchAllField(String searchTerm, Collection<String> entityNames);

    List<EntityInfo> searchLinksField(Object id, int maxResults);

    List<EntityInfo> searchLinksField(Object id, List<String> entityNames);

    void close();
}
