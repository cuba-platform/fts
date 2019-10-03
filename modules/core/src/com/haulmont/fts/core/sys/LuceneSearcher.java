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

import com.haulmont.fts.global.EntityInfo;

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
