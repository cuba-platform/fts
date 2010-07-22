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
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.global.MetadataProvider;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.*;

public class LuceneIndexer extends LuceneWriter {

    private static Log log = LogFactory.getLog(LuceneIndexer.class);

    private Map<String, EntityDescr> descriptions;

    private List<Pair<String, UUID>> deleteQueue = new ArrayList<Pair<String, UUID>>();

    public LuceneIndexer(Map<String, EntityDescr> descriptions, Directory directory) {
        super(directory);
        this.descriptions = descriptions;
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
            PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_30));

            Transaction tx = Locator.createTransaction();
            try {
                EntityManager em = PersistenceProvider.getEntityManager();
                MetaClass metaClass = MetadataProvider.getSession().getClass(entityName);
                Entity entity = em.find(metaClass.getJavaClass(), entityId);

                idField = new Field(FLD_ID, entityId.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED);

                entityField = new Field(FLD_ENTITY, entityName, Field.Store.YES, Field.Index.NOT_ANALYZED);

                allField = new Field(FLD_ALL, createAllFieldContent(entity, descr), Field.Store.NO, Field.Index.ANALYZED);

                linksField = new Field(FLD_LINKS, createLinksFieldContent(entity, descr), Field.Store.YES, Field.Index.NOT_ANALYZED);
                analyzer.addAnalyzer(FLD_LINKS, new WhitespaceAnalyzer());

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
                log.debug("Updating document " + entityId);
                writer.updateDocument(new Term(FLD_ID, entityId.toString()), doc);
            } else {
                log.debug("Adding document " + entityId);
                writer.addDocument(doc);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String createAllFieldContent(Entity entity, EntityDescr descr) {
        StringBuilder sb = new StringBuilder();

        for (String propName : descr.getLocalProperties()) {
            Object value = ((Instance) entity).getValue(propName);
            if (value != null && !StringUtils.isBlank(value.toString())) {
                appendString(sb, value);
            }
        }
        if (log.isTraceEnabled())
            log.trace("Entity " + entity.getId() + " all field: " + sb.toString());

        return sb.toString();
    }

    private String createLinksFieldContent(Entity entity, EntityDescr descr) {
        StringBuilder sb = new StringBuilder();

        for (String propName : descr.getLinkProperties()) {
            addLinkedPropertyEx(sb, (Instance) entity, InstanceUtils.parseValuePath(propName));
        }
        if (log.isTraceEnabled())
            log.trace("Entity " + entity.getId() + " links field: " + sb.toString());

        return sb.toString();
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
