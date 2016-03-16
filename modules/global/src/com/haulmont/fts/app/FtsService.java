/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.app;

import com.haulmont.fts.global.SearchResult;

import java.util.List;

public interface FtsService {

    String NAME = "cuba_FtsService";

    /**
     * Performs a full text search among all entities described in fts
     * configuration file. Number of entities in result is restricted by
     * {@link com.haulmont.cuba.core.global.FtsConfig#getMaxSearchResults()}
     */
    SearchResult search(String searchTerm);

    /**
     * Performs a full text search. SearchResult will contain only entities with
     * names passed in {@code entityNames} parameter.
     * <p>Please notice that the result will contain all entities that match a search criteria</p>
     */
    SearchResult search(String searchTerm, List<String> entityNames);

    SearchResult expandResult(SearchResult result, String entityName);

    /**
     * Checks whether an entity is indexed by full text search engine
     */
    boolean isEntityIndexed(String entityName);

    /**
     * @return a list of entity names that contains entity name from parameter itself,
     * names of entity descendants and name of original meta class if passed entity is an extension
     */
    List<String> collectEntityHierarchyNames(String entityName);

    /**
     * @return a caption for entity property that was found by fts
     */
    String getHitPropertyCaption(String entityName, String hitProperty);
}
