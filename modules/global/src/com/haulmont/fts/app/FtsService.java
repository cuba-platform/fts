/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.07.2010 15:46:46
 *
 * $Id$
 */
package com.haulmont.fts.app;

import com.haulmont.fts.global.SearchResult;

public interface FtsService {

    String NAME = "cuba_FtsService";

    SearchResult search(String searchTerm);

    SearchResult expandResult(SearchResult result, String entityName);
}
