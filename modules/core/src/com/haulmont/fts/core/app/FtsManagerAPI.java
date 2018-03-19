/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.fts.core.sys.EntityDescr;
import com.haulmont.fts.core.sys.LuceneSearcherAPI;
import org.apache.lucene.store.Directory;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Provides FTS service functions.
 */
public interface FtsManagerAPI {

    String NAME = "cuba_FtsManager";

    Directory getDirectory();

    List<Entity> getSearchableEntities(Entity entity);

    boolean isReindexing();

    Queue<String> getReindexEntitiesQueue();

    Map<String, EntityDescr> getDescrByName();

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

    LuceneSearcherAPI getSearcher();
}
