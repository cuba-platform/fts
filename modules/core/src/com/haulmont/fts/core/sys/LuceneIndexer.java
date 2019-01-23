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
