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
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;

import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Provides FTS service functions.
 */
public interface FtsManagerAPI {

    String NAME = "cuba_FtsManager";

    List<Entity> getSearchableEntities(Entity entity);

    boolean isReindexing();

    Queue<String> getReindexEntitiesQueue();

    int processQueue();

    String optimize();

    String upgrade();

    boolean showInResults(String entityName);

    boolean isEnabled();

    void setEnabled(boolean value);

    boolean isWriting();

    void deleteIndexForEntity(String entityName);

    void deleteIndex();

    int reindexEntity(String entityName);

    int reindexAll();

    void asyncReindexEntity(String entityName);

    void asyncReindexAll();

    int reindexNextBatch();

    /**
     * Method checks whether entities of the given MetaClass can be indexed. For example, indexing of entities that
     * have composite primary key and don't implement {@link com.haulmont.cuba.core.entity.HasUuid} interface is not
     * possible.
     */
    boolean isEntityCanBeIndexed(MetaClass metaClass);

    /**
     * From FTS point of view there are cases when non-PK field must be treated as primary key for building JPQL
     * queries, for example.
     * <p>
     * When indexing or performing a full-text search on an entity with composite key, its 'uuid' field (if presented)
     * must be used instead of its real primary key (embedded entity).
     *
     * @return a MetaProperty with a primary key or a MetaProperty for 'uuid' field (in case of entity with composite
     * primary key
     */
    MetaProperty getPrimaryKeyPropertyForFts(MetaClass metaClass);

    /**
     * @return current index format version
     */
    String getIndexFormatVersion();

    /**
     * @return last index format version supported by lucene
     */
    String getLatestIndexFormatVersion();
}
