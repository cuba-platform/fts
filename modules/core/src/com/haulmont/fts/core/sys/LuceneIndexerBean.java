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

import com.google.common.base.Strings;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.TransactionParams;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.entity.HasUuid;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.fts.global.EntityInfo;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.FtsConfig;
import com.haulmont.fts.global.ValueFormatter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.odf.OpenDocumentParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.rtf.RTFParser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.haulmont.fts.core.sys.LuceneConstants.*;

@Component(LuceneIndexer.NAME)
public class LuceneIndexerBean implements LuceneIndexer {

    private static final Logger log = LoggerFactory.getLogger(LuceneIndexerBean.class);

    private ValueFormatter valueFormatter = new ValueFormatter();

    @Inject
    protected FtsConfig ftsConfig;

    @Inject
    protected Persistence persistence;

    @Inject
    protected com.haulmont.cuba.core.global.Metadata metadata;

    protected List<DocumentCreatedListener> documentCreatedListeners = new ArrayList<>();

    @Inject
    protected IndexWriterProvider indexWriterProvider;

    @Inject
    protected EntityDescrsManager entityDescrsManager;

    @Inject
    protected IndexSearcherProvider indexSearcherProvider;

    @Override
    public void indexEntity(String entityName, Object entityId, FtsChangeType changeType, IndexWriter writer) throws IndexingException {
        if (FtsChangeType.DELETE.equals(changeType)) {
            try {
                deleteDocument(entityName, entityId, writer);
            } catch (IOException e) {
                log.error("Error deleting {}-{}", entityName, entityId);
                throw new RuntimeException("Error on deleting a document from the Lucene index", e);
            }
            return;
        }
        try {
            EntityDescr entityDescr = entityDescrsManager.getDescrByEntityName(entityName);
            if (entityDescr == null) {
                log.error("No description for entity {}", entityName);
                return;
            }

            Document doc;
            MetaClass metaClass = metadata.getSession().getClassNN(entityName);
            String storeName = metadata.getTools().getStoreName(metaClass);
            try (Transaction tx = persistence.createTransaction(storeName, new TransactionParams().setReadOnly(true))) {
                EntityManager em = persistence.getEntityManager(storeName);
                Entity entity;

                if (metadata.getTools().hasCompositePrimaryKey(metaClass) && HasUuid.class.isAssignableFrom(metaClass.getJavaClass())) {
                    entity = (Entity) em.createQuery("select e from " + metaClass.getName() + " e where e.uuid = :uuid")
                            .setParameter("uuid", entityId)
                            .getFirstResult();
                } else {
                    entity = em.find(metaClass.getJavaClass(), entityId);
                }
                if (entity == null) {
                    log.info("Entity instance not found (could be deleted): {}-{}", entityName, entityId);
                    return;
                }

                Field idField = new StringField(FLD_ID, entityId.toString(), Field.Store.YES);

                Field entityField = new StringField(FLD_ENTITY, entityName, Field.Store.YES);

                String allContent = createAllFieldContent(entity, entityDescr);

                Field allField = new TextField(
                        FLD_ALL,
                        allContent,
                        ftsConfig.getStoreContentInIndex() ? Field.Store.YES : Field.Store.NO
                );

                Field morphologyAllField = new TextField(
                        FLD_MORPHOLOGY_ALL,
                        allContent,
                        Field.Store.NO
                );

                Field linksField = new TextField(
                        FLD_LINKS,
                        createLinksFieldContent(entity, entityDescr),
                        Field.Store.YES
                );

                doc = new Document();
                doc.add(idField);
                doc.add(entityField);
                doc.add(allField);
                doc.add(linksField);
                doc.add(morphologyAllField);
                documentCreated(doc, entity, entityDescr);

                tx.commit();
            }

            if (FtsChangeType.UPDATE.equals(changeType)) {
                log.debug("Updating document {}-{}", entityName, entityId);
                deleteDocument(entityName, entityId, writer);
                writer.addDocument(doc);
            } else {
                log.debug("Adding document {}-{}", entityName, entityId);
                writer.addDocument(doc);
            }

        } catch (IndexingException e) {
            log.error("Error indexing {}-{}", entityName, entityId);
            throw new IndexingException(entityName, entityId, e.getEntityType(), e);
        } catch (IOException e) {
            log.error("Error indexing {}-{}", entityName, entityId);
            throw new IndexingException(entityName, entityId, IndexingException.EntityType.OTHER, e);
        } catch (RuntimeException e) {
            log.error("Error indexing {}-{}", entityName, entityId);
            throw e;
        }
    }

    protected void deleteDocument(String entityName, Object entityId, IndexWriter writer) throws IOException {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        Term entityNameTerm = new Term(FLD_ENTITY, entityName);
        Term idTerm = new Term(FLD_ID, entityId.toString());
        queryBuilder.add(new TermQuery(idTerm), BooleanClause.Occur.MUST);
        queryBuilder.add(new TermQuery(entityNameTerm), BooleanClause.Occur.MUST);
        writer.deleteDocuments(queryBuilder.build());
    }

    /**
     * Example of the "all" field content:
     * <ul>
     *     <li>^^description Description of the book goes here ^^title War and Peace</li>
     * </ul>
     */
    protected String createAllFieldContent(Entity entity, EntityDescr descr) throws IndexingException {
        StringBuilder sb = new StringBuilder();

        for (String propName : descr.getLocalProperties()) {
            Object value = entity.getValueEx(propName); // using getValueEx() to support embedded entities

            String str = valueFormatter.format(value);
            if (str != null && !StringUtils.isBlank(str)) {
                if (ftsConfig.getStoreContentInIndex()) {
                    appendString(sb, makeFieldName(propName));
                }
                appendString(sb, str);
            }
        }
        if (entity instanceof FileDescriptor && ftsConfig.getFileContentIndexingEnabled()) {
            appendString(sb, makeFieldName(FTS.FILE_CONT_PROP));
            sb.append(FTS.FIELD_SEP).append(((FileDescriptor) entity).getName().replaceAll("\\s+", FTS.FIELD_SEP));
            appendFileContent(sb, (FileDescriptor) entity);
        }

        if (log.isTraceEnabled())
            log.trace("Entity {} all field: {}", entity, sb.toString());

        return sb.toString();
    }

    protected void appendFileContent(StringBuilder sb, FileDescriptor fileDescriptor) throws IndexingException {
        Parser parser = getParser(fileDescriptor);
        if (parser == null) return;
        FileStorageAPI fs = AppBeans.get(FileStorageAPI.class);
        byte[] data;
        try {
            data = fs.loadFile(fileDescriptor);
        } catch (FileStorageException e) {
            throw new IndexingException(IndexingException.EntityType.FILE, e);
        }
        InputStream stream = new ByteArrayInputStream(data);

        StringWriter stringWriter = new StringWriter();
        try {
            parser.parse(stream, new BodyContentHandler(stringWriter), new Metadata(), new ParseContext());
        } catch (OfficeXmlFileException e) {
            if (parser instanceof OfficeParser) {
                parser = new OOXMLParser();
                try {
                    stream = new ByteArrayInputStream(data);
                    stringWriter = new StringWriter();
                    parser.parse(stream, new BodyContentHandler(stringWriter), new Metadata(), new ParseContext());
                } catch (Exception e1) {
                    throw new IndexingException(IndexingException.EntityType.FILE, e);
                }
            } else {
                throw new IndexingException(IndexingException.EntityType.FILE, e);
            }
        } catch (Exception e) {
            throw new IndexingException(IndexingException.EntityType.FILE, e);
        }
        appendString(sb, stringWriter.toString());
    }

    protected Parser getParser(FileDescriptor fileDescriptor) {
        Parser parser;
        String ext = fileDescriptor.getExtension();
        if (Strings.isNullOrEmpty(ext)) {
            log.warn("Unable to create a parser for a file without extension");
            return null;
        }
        switch (ext) {
            case "pdf":
                parser = new PDFParser();
                break;
            case "doc":
            case "xls":
                parser = new OfficeParser();
                break;
            case "docx":
            case "xlsx":
                parser = new OOXMLParser();
                break;
            case "odt":
            case "ods":
                parser = new OpenDocumentParser();
                break;
            case "rtf":
                parser = new RTFParser();
                break;
            case "txt":
                parser = new TXTParser();
                break;
            default:
                log.warn("Unsupported file extension: {}", ext);
                return null;
        }
        return parser;
    }

    /**
     * Examples of the "links" field:
     * <ul>
     *     <li><i>^^ebook sys$FileDescriptor-bfec4205-11e1-3c7e-9644-7eee3a732fb2 ^^author sample_Author-0872af91-17d4-304e-85b4-b871aa9e41f6</i></li>
     *     <li><i>^^attachments^file sys$FileDescriptor-3f659ed4-2d7c-895b-fefc-4f8936c2a80d sys$FileDescriptor-4e8f45c3-64d5-6346-c7ba-6697adfe85db</i></li>
     * </ul>
     */
    protected String createLinksFieldContent(Entity entity, EntityDescr descr) {
        StringBuilder sb = new StringBuilder();

        for (String propName : descr.getLinkProperties()) {
            if (ftsConfig.getStoreContentInIndex()) {
                appendString(sb, makeFieldName(propName));
            }
            addLinkedPropertyEx(sb, entity, InstanceUtils.parseValuePath(propName));
        }
        if (log.isTraceEnabled()) {
            log.trace("Entity {} links field: {}", entity, sb.toString());
        }

        return sb.toString();
    }

    protected String makeFieldName(String propName) {
        return FTS.FIELD_START + propName.replace(".", FTS.FIELD_SEP);
    }

    protected void addLinkedPropertyEx(StringBuilder sb, Entity entity, String[] propertyPath) {
        String prop = propertyPath[0];
        Object value = entity.getValue(prop);
        if (value instanceof Entity) {
            if (propertyPath.length == 1) {
                String originalMetaClassName = metadata.getExtendedEntities().getEffectiveMetaClass(((Entity) value).getMetaClass()).getName();
                String entityInfoStr = new EntityInfo(originalMetaClassName, ((Entity) value).getId()).toString();
                appendString(sb, entityInfoStr);
            } else {
                addLinkedPropertyEx(sb, (Entity) value, (String[]) ArrayUtils.subarray(propertyPath, 1, propertyPath.length));
            }
        } else if (value instanceof Collection && !((Collection) value).isEmpty()) {
            Collection<Entity> collection = (Collection<Entity>) value;
            for (Entity inst : collection) {
                if (inst != null) {
                    if (propertyPath.length == 1) {
                        String originalMetaClassName = metadata.getExtendedEntities().getEffectiveMetaClass(inst.getMetaClass()).getName();
                        String entityInfoStr = new EntityInfo(originalMetaClassName, inst.getId()).toString();
                        appendString(sb, entityInfoStr);
                    } else {
                        addLinkedPropertyEx(sb, inst, (String[]) ArrayUtils.subarray(propertyPath, 1, propertyPath.length));
                    }
                }
            }
        }
    }

    protected void appendString(StringBuilder sb, Object obj) {
        if (sb.length() > 0)
            sb.append(" ");
        sb.append(obj.toString());
    }

    @Override
    public void addListener(DocumentCreatedListener documentCreatedListener) {
        this.documentCreatedListeners.add(documentCreatedListener);
    }

    protected void documentCreated(Document document, Entity entity, EntityDescr descr) {
        for (DocumentCreatedListener documentCreatedListener : documentCreatedListeners) {
            documentCreatedListener.onDocumentCreated(document, entity, descr);
        }
    }

    @Override
    public void deleteAllDocuments() {
        try {
            IndexWriter writer = indexWriterProvider.getIndexWriter();
            writer.deleteAll();
            writer.commit();
            indexSearcherProvider.getSearcherManager().maybeRefresh();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDocumentsForEntity(String entityName) {
        try {
            IndexWriter writer = indexWriterProvider.getIndexWriter();
            writer.deleteDocuments(new Term(FLD_ENTITY, entityName));
            writer.commit();
            indexSearcherProvider.getSearcherManager().maybeRefresh();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
