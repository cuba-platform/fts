/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.app;

import com.haulmont.fts.global.SearchResult;

public interface FtsService {

    String NAME = "cuba_FtsService";

    SearchResult search(String searchTerm);

    SearchResult expandResult(SearchResult result, String entityName);
}
