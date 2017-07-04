/*
 * Copyright (c) 2008-2017 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import java.util.Collection;
import java.util.List;

/**
 */
public interface LuceneSearcherAPI {
    String NAME = "fts_LuceneSearcher";

    boolean isCurrent();

    List<EntityInfo> searchAllField(String searchTerm, int maxResults);

    List<EntityInfo> searchAllField(String searchTerm, Collection<String> entityNames);

    List<EntityInfo> searchLinksField(Object id, int maxResults);

    List<EntityInfo> searchLinksField(Object id, List<String> entityNames);
}
