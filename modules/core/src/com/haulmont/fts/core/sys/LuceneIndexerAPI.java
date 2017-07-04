/*
 * Copyright (c) 2008-2017 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FtsChangeType;
import org.apache.lucene.document.Document;

/**
 */
public interface LuceneIndexerAPI {

    String NAME = "fts_LuceneIndexer";

    void indexEntity(String entityName, Object entityId, FtsChangeType changeType) throws IndexingException;

    void close();

    void addListener(DocumentCreatedListener documentCreatedListener);

    interface DocumentCreatedListener {
        void onDocumentCreated(Document document, Entity entity, EntityDescr descr);
    }
}
