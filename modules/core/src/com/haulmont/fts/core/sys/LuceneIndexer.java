/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.bali.datastruct.Pair;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.entity.HasUuid;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.ValueFormatter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

public class LuceneIndexer extends LuceneWriter {

    private static final Logger log = LoggerFactory.getLogger(LuceneIndexer.class);

    private Map<String, EntityDescr> descriptions;

    private boolean storeContentInIndex;

    private List<Pair<String, UUID>> deleteQueue = new ArrayList<>();

    private ValueFormatter valueFormatter;

    protected Persistence persistence;

    protected com.haulmont.cuba.core.global.Metadata metadata;

    protected List<DocumentCreatedListener> documentCreatedListeners = new ArrayList<>();

    public LuceneIndexer(Map<String, EntityDescr> descriptions, Directory directory, boolean storeContentInIndex) {
        super(directory);
        this.descriptions = descriptions;
        this.storeContentInIndex = storeContentInIndex;

        valueFormatter = new ValueFormatter();
        persistence = AppBeans.get(Persistence.NAME);
        metadata = AppBeans.get(com.haulmont.cuba.core.global.Metadata.NAME);
    }

    @Override
    public void close() {
        try {
            if (!deleteQueue.isEmpty()) {
                log.debug("Deleting documents {}", deleteQueue);

                for (Pair<String, UUID> pair : deleteQueue) {
                    writer.deleteDocuments(new Term(FLD_ID, pair.getSecond().toString()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            super.close();
        }
    }

    public void indexEntity(String entityName, UUID entityId, FtsChangeType changeType) throws IndexingException {
        if (FtsChangeType.DELETE.equals(changeType)) {
            deleteQueue.add(new Pair<>(entityName, entityId));
            return;
        }
        try {
            EntityDescr descr = descriptions.get(entityName);
            if (descr == null) {
                log.error("No description for entity " + entityName);
                return;
            }

            Field idField, entityField, allField, linksField, morphologyAllField;
            Entity entity;
            Document doc;
            Transaction tx = persistence.createTransaction();
            try {
                EntityManager em = persistence.getEntityManager();
                MetaClass metaClass = metadata.getSession().getClass(entityName);
                entity = em.find(metaClass.getJavaClass(), entityId);
                if (entity == null) {
                    log.error("Entity instance not found: " + entityName + "-" + entityId);
                    return;
                }

                idField = new StringField(FLD_ID, entityId.toString(), Field.Store.YES);

                entityField = new StringField(FLD_ENTITY, entityName, Field.Store.YES);

                String allContent = createAllFieldContent(entity, descr);

                allField = new TextField(
                        FLD_ALL,
                        allContent,
                        storeContentInIndex ? Field.Store.YES : Field.Store.NO
                );

                morphologyAllField = new TextField(
                        FLD_MORPHOLOGY_ALL,
                        allContent,
                        Field.Store.NO
                );

                linksField = new TextField(
                        FLD_LINKS,
                        createLinksFieldContent(entity, descr),
                        Field.Store.YES
                );

                doc = new Document();
                doc.add(idField);
                doc.add(entityField);
                doc.add(allField);
                doc.add(linksField);
                doc.add(morphologyAllField);
                documentCreated(doc, entity, descr);

                tx.commit();
            } finally {
                tx.end();
            }

            if (FtsChangeType.UPDATE.equals(changeType)) {
                log.debug("Updating document " + entityName + "-" + entityId);
                writer.updateDocument(new Term(FLD_ID, entityId.toString()), doc);
            } else {
                log.debug("Adding document " + entityName + "-" + entityId);
                writer.addDocument(doc);
            }

        } catch (IndexingException e) {
            log.error("Error indexing " + entityName + "-" + entityId);
            throw new IndexingException(entityName, entityId, e.getEntityType(), e);
        } catch (IOException e) {
            log.error("Error indexing " + entityName + "-" + entityId);
            throw new IndexingException(entityName, entityId, IndexingException.EntityType.OTHER, e);
        } catch (RuntimeException e) {
            log.error("Error indexing " + entityName + "-" + entityId);
            throw e;
        }
    }

    protected String createAllFieldContent(Entity entity, EntityDescr descr) throws IndexingException {
        StringBuilder sb = new StringBuilder();

        for (String propName : descr.getLocalProperties()) {
            Object value = entity.getValueEx(propName); // using getValueEx() to support embedded entities

            String str = valueFormatter.format(value);
            if (str != null && !StringUtils.isBlank(str)) {
                if (storeContentInIndex) {
                    appendString(sb, makeFieldName(propName));
                }
                appendString(sb, str);
            }
        }
        if (entity instanceof FileDescriptor) {
            appendString(sb, makeFieldName(FTS.FILE_CONT_PROP));
            sb.append(FTS.FIELD_SEP).append(((FileDescriptor) entity).getName().replaceAll("\\s+", FTS.FIELD_SEP));
            appendFileContent(sb, (FileDescriptor) entity);
        }

        if (log.isTraceEnabled())
            log.trace("Entity " + entity + " all field: " + sb.toString());

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
                log.warn("Unsupported file extension: " + ext);
                return null;
        }
        return parser;
    }

    private String createLinksFieldContent(Entity entity, EntityDescr descr) {
        StringBuilder sb = new StringBuilder();

        for (String propName : descr.getLinkProperties()) {
            if (storeContentInIndex) {
                appendString(sb, makeFieldName(propName));
            }
            addLinkedPropertyEx(sb, entity, InstanceUtils.parseValuePath(propName));
        }
        if (log.isTraceEnabled())
            log.trace("Entity " + entity + " links field: " + sb.toString());

        return sb.toString();
    }

    private String makeFieldName(String propName) {
        return FTS.FIELD_START + propName.replace(".", FTS.FIELD_SEP);
    }

    private void addLinkedPropertyEx(StringBuilder sb, Instance instance, String[] propertyPath) {
        String prop = propertyPath[0];
        Object value = instance.getValue(prop);
        if (value instanceof Instance && value instanceof HasUuid) {
            if (propertyPath.length == 1) {
                appendString(sb, ((HasUuid) value).getUuid());
            } else {
                addLinkedPropertyEx(sb, (Instance) value, (String[]) ArrayUtils.subarray(propertyPath, 1, propertyPath.length));
            }
        } else if (value instanceof Collection && !((Collection) value).isEmpty()) {
            Collection<Instance> collection = (Collection<Instance>) value;
            for (Instance inst : collection) {
                if (inst instanceof HasUuid) {
                    if (propertyPath.length == 1) {
                        appendString(sb, ((HasUuid) inst).getUuid());
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

    public void addListener(DocumentCreatedListener documentCreatedListener) {
        this.documentCreatedListeners.add(documentCreatedListener);
    }

    protected void documentCreated(Document document, Entity entity, EntityDescr descr) {
        for (DocumentCreatedListener documentCreatedListener : documentCreatedListeners) {
            documentCreatedListener.onDocumentCreated(document, entity, descr);
        }
    }

    public interface DocumentCreatedListener {
        void onDocumentCreated(Document document, Entity entity, EntityDescr descr);
    }
}
