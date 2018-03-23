/*
 * Copyright (c) 2008-2017 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FtsChangeType;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

/**
 * INTERNAL
 * <p>
 * Bean contains operations for working with lucene documents for CUBA entities.
 */
public interface LuceneIndexer {

    String NAME = "fts_LuceneIndexer";

    void indexEntity(String entityName, Object entityId, FtsChangeType changeType, IndexWriter writer) throws IndexingException;

    void addListener(DocumentCreatedListener documentCreatedListener);

    void deleteAllDocuments();

    void deleteDocumentsForEntity(String entityName);

    interface DocumentCreatedListener {
        void onDocumentCreated(Document document, Entity entity, EntityDescr descr);
    }
}
