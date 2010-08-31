/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 21.07.2010 12:15:13
 *
 * $Id$
 */
package com.haulmont.fts.core.sys;

import com.haulmont.bali.datastruct.Pair;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Locator;
import com.haulmont.cuba.core.PersistenceProvider;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.MetadataProvider;
import com.haulmont.fts.global.FTS;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.tika.exception.TikaException;
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
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

public class LuceneIndexer extends LuceneWriter {

    private static Log log = LogFactory.getLog(LuceneIndexer.class);

    private Map<String, EntityDescr> descriptions;

    private boolean storeContentInIndex;

    private List<Pair<String, UUID>> deleteQueue = new ArrayList<Pair<String, UUID>>();

    private ValueFormatter valueFormatter;

    public LuceneIndexer(Map<String, EntityDescr> descriptions, Directory directory, boolean storeContentInIndex) {
        super(directory);
        this.descriptions = descriptions;
        this.storeContentInIndex = storeContentInIndex;

        valueFormatter = new ValueFormatter();
    }

    public void close() {
        super.close();
        try {
            if (!deleteQueue.isEmpty()) {
                log.debug("Deleting documents " + deleteQueue);
                IndexReader indexReader = IndexReader.open(directory, false);
                for (Pair<String, UUID> pair : deleteQueue) {
                    indexReader.deleteDocuments(new Term(FLD_ID, pair.getSecond().toString()));
                }
                indexReader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void indexEntity(String entityName, UUID entityId, FtsChangeType changeType) {
        if (FtsChangeType.DELETE.equals(changeType)) {
            deleteQueue.add(new Pair<String, UUID>(entityName, entityId));
            return;
        }
        try {
            EntityDescr descr = descriptions.get(entityName);
            if (descr == null) {
                log.error("No description for entity " + entityName);
                return;
            }

            Field idField, entityField, allField, linksField;
            Transaction tx = Locator.createTransaction();
            try {
                EntityManager em = PersistenceProvider.getEntityManager();
                MetaClass metaClass = MetadataProvider.getSession().getClass(entityName);
                Entity entity = em.find(metaClass.getJavaClass(), entityId);
                if (entity == null) {
                    log.error("Entity instance not found: " + entityName + "-" + entityId);
                    return;
                }

                idField = new Field(FLD_ID, entityId.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED);

                entityField = new Field(FLD_ENTITY, entityName, Field.Store.YES, Field.Index.NOT_ANALYZED);

                allField = new Field(
                        FLD_ALL,
                        createAllFieldContent(entity, descr),
                        storeContentInIndex ? Field.Store.YES : Field.Store.NO,
                        Field.Index.ANALYZED
                );

                linksField = new Field(
                        FLD_LINKS,
                        createLinksFieldContent(entity, descr),
                        Field.Store.YES,
                        Field.Index.ANALYZED
                );

                tx.commit();
            } finally {
                tx.end();
            }

            Document doc = new Document();
            doc.add(idField);
            doc.add(entityField);
            doc.add(allField);
            doc.add(linksField);

            if (FtsChangeType.UPDATE.equals(changeType)) {
                log.debug("Updating document " + entityName + "-" + entityId);
                writer.updateDocument(new Term(FLD_ID, entityId.toString()), doc);
            } else {
                log.debug("Adding document " + entityName + "-" + entityId);
                writer.addDocument(doc);
            }

        } catch (IOException e) {
            log.error("Error indexing "  + entityName + "-" + entityId);
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            log.error("Error indexing "  + entityName + "-" + entityId);
            throw e;
        }
    }

    private String createAllFieldContent(Entity entity, EntityDescr descr) {
        StringBuilder sb = new StringBuilder();

        for (String propName : descr.getLocalProperties()) {
            Object value = ((Instance) entity).getValue(propName);

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
            appendFileContent(sb, ((FileDescriptor) entity));
        }

        if (log.isTraceEnabled())
            log.trace("Entity " + entity + " all field: " + sb.toString());

        return sb.toString();
    }

    private void appendFileContent(StringBuilder sb, FileDescriptor fileDescriptor) {
        FileStorageAPI fs = Locator.lookup(FileStorageAPI.NAME);
        byte[] data;
        try {
            data = fs.loadFile(fileDescriptor);
        } catch (FileStorageException e) {
            log.error("Error indexing file " + fileDescriptor.getFileName() + ": " + e.getMessage());
            return;
        }
        InputStream stream = new ByteArrayInputStream(data);
        Parser parser;
        String ext = fileDescriptor.getExtension();
        if ("pdf".equals(ext))
            parser = new PDFParser();
        else if ("doc".equals(ext) || "xls".equals(ext))
            parser = new OfficeParser();
        else if ("docx".equals(ext) || "xlsx".equals(ext))
            parser = new OOXMLParser();
        else if ("odt".equals(ext) || "ods".equals(ext))
            parser = new OpenDocumentParser();
        else if ("rtf".equals(ext))
            parser = new RTFParser();
        else if ("txt".equals(ext))
            parser = new TXTParser();
        else {
            log.warn("Unsupported file extension: " + ext);
            return;
        }

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
                    log.error("Error indexing file " + fileDescriptor.getFileName(), e1);
                    return;
                }
            } else {
                log.error("Error indexing file " + fileDescriptor.getFileName(), e);
                return;
            }
        } catch (Exception e) {
            log.error("Error indexing file " + fileDescriptor.getFileName(), e);
            return;
        }
        appendString(sb, stringWriter.toString());
    }

    private String createLinksFieldContent(Entity entity, EntityDescr descr) {
        StringBuilder sb = new StringBuilder();

        for (String propName : descr.getLinkProperties()) {
            if (storeContentInIndex) {
                appendString(sb, makeFieldName(propName));
            }
            addLinkedPropertyEx(sb, (Instance) entity, InstanceUtils.parseValuePath(propName));
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
        if (value instanceof Instance) {
            if (propertyPath.length == 1) {
                appendString(sb, ((Instance) value).getUuid());
            } else {
                addLinkedPropertyEx(sb, (Instance) value, (String[]) ArrayUtils.subarray(propertyPath, 1, propertyPath.length));
            }
        } else if (value instanceof Collection && !((Collection) value).isEmpty()) {
            Collection<Instance> collection = (Collection<Instance>) value;
            for (Instance inst : collection) {
                if (propertyPath.length == 1) {
                    appendString(sb, inst.getUuid());
                } else {
                    addLinkedPropertyEx(sb, inst, (String[]) ArrayUtils.subarray(propertyPath, 1, propertyPath.length));
                }
            }
        }
    }

    private void appendString(StringBuilder sb, Object obj) {
        if (sb.length() > 0)
            sb.append(" ");
        sb.append(obj.toString());
    }
}
