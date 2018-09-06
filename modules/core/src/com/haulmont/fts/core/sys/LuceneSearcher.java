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
public interface LuceneSearcher {

    String NAME = "fts_LuceneSearcher";

    default List<EntityInfo> searchAllField(String searchTerm, int maxResults) {
        return searchAllField(searchTerm, 0, maxResults);
    }

    List<EntityInfo> searchAllField(String searchTerm, int firstResult, int maxResults);

    List<EntityInfo> searchAllField(String searchTerm, Collection<String> entityNames);

    default List<EntityInfo> searchLinksField(Object id, int maxResults) {
        return searchLinksField(id, 0, maxResults);
    }

    List<EntityInfo> searchLinksField(Object id, int firstResult, int maxResults);

    List<EntityInfo> searchLinksField(Object id, List<String> entityNames);
}
