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
package com.haulmont.fts.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.fts.global.QueryKey;
import com.haulmont.fts.global.SearchResult;

import java.util.List;

/**
 * Provides search functionality to clients.
 */
public interface FtsService {

    String NAME = "cuba_FtsService";

    /**
     * Performs a full text search among all entities described in fts configuration file. Number of entities in the result is restricted by {@link
     * com.haulmont.fts.global.FtsConfig#getMaxSearchResults()}
     */
    default SearchResult search(String searchTerm) {
        return search(searchTerm, (QueryKey) null, FtsSearchOption.POPULATE_HIT_INFOS);
    }

    /**
     * Performs a full text search among all entities described in fts configuration file. Number of entities in the result is restricted by {@link
     * com.haulmont.fts.global.FtsConfig#getMaxSearchResults()}
     * <p>
     * If the {@code searchOptions} contains the {@link FtsSearchOption#POPULATE_HIT_INFOS} value then all {@link
     * com.haulmont.fts.global.SearchResultEntry} of the {@code SearchResult} will contain {@code hitInfos} collection filled.
     */
    SearchResult search(String searchTerm, QueryKey queryKey, FtsSearchOption... searchOptions);

    /**
     * Performs a full text search. SearchResult will contain only entities with names passed in {@code entityNames} parameter.
     * <p>
     * Please notice that the result will contain all entities that match a search criteria
     * <p>
     * The {@code hitInfos} collection of the {@link com.haulmont.fts.global.SearchResultEntry} WILL NOT be filled.
     */
    SearchResult search(String searchTerm, List<String> entityNames);

    /**
     * Checks whether an entity is indexed by full text search engine
     */
    boolean isEntityIndexed(String entityName);

    /**
     * @return a list of entity names that contains entity name from parameter itself, names of entity descendants and name of original meta class if
     * passed entity is an extension
     */
    List<String> collectEntityHierarchyNames(String entityName);

    /**
     * @return a caption for entity property that was found by fts
     */
    String getHitPropertyCaption(String entityName, String hitProperty);

    /**
     * See {@code FtsManagerAPI#getPrimaryKeyPropertyForFts()} javadoc for details
     */
    MetaProperty getPrimaryKeyPropertyForFts(MetaClass metaClass);
}