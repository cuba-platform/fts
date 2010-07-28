/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 24.06.2010 18:18:38
 *
 * $Id$
 */
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.app.FtsSender;
import com.haulmont.cuba.core.app.ManagementBean;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.entity.FtsQueue;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.fts.core.sys.ConfigLoader;
import com.haulmont.fts.core.sys.EntityDescr;
import com.haulmont.fts.core.sys.LuceneIndexer;
import com.haulmont.fts.core.sys.LuceneWriter;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@ManagedBean(FtsManagerAPI.NAME)
public class FtsManager extends ManagementBean implements FtsManagerAPI, FtsManagerMBean {

    private static Log log = LogFactory.getLog(FtsManager.class);

    private volatile Map<String, EntityDescr> descrByClassName;
    private volatile Map<String, EntityDescr> descrByName;

    private ReentrantLock writeLock = new ReentrantLock();
    private volatile boolean writing;
    private volatile int writeCount;

    private volatile Directory directory;

    private static final int DEL_CHUNK = 10;

    private FtsConfig config;

    @Inject
    public void setConfigProvider(ConfigProvider configProvider) {
        config = configProvider.doGetConfig(FtsConfig.class);
    }

    public boolean isWriting() {
        return writing;
    }

    private Map<String, EntityDescr> getDescrByClassName() {
        if (descrByClassName == null) {
            synchronized (this) {
                if (descrByClassName == null) {
                    ConfigLoader loader = new ConfigLoader();
                    descrByClassName = loader.loadConfiguration();
                }
            }
        }
        return descrByClassName;
    }

    private Map<String, EntityDescr> getDescrByName() {
        if (descrByName == null) {
            synchronized (this) {
                if (descrByName == null) {
                    descrByName = new HashMap<String, EntityDescr>(getDescrByClassName().size());
                    for (EntityDescr descr : getDescrByClassName().values()) {
                        String name = descr.getMetaClass().getName();
                        descrByName.put(name, descr);
                    }
                }
            }
        }
        return descrByName;
    }

    public List<BaseEntity> getSearchableEntities(BaseEntity entity) {
        List<BaseEntity> list = new ArrayList<BaseEntity>();

        EntityDescr descr = getDescrByClassName().get(entity.getClass().getName());
        if (descr == null)
            return list;

        Set<String> properties = descr.getPropertyNames();

        Set<String> ownProperties = new HashSet<String>(properties.size());
        for (String property : properties) {
            String p = property.indexOf(".") < 0 ? property : property.substring(0, property.indexOf("."));
            ownProperties.add(p);
        }

        Set<String> dirty = PersistenceProvider.getDirtyFields(entity);
        for (String s : dirty) {
            if (ownProperties.contains(s)) {
                if (StringUtils.isBlank(descr.getSearchableIfScript())) {
                    list.add(entity);
                } else if (runSearchableIf(entity, descr)) {
                    list.add(entity);
                }
                break;
            }
        }
        
        if (!StringUtils.isBlank(descr.getSearchablesScript())) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("entity", entity);
            params.put("searchables", list);
            ScriptingProvider.evaluateGroovy(ScriptingProvider.Layer.CORE, descr.getSearchablesScript(), params);
        }

        return list;
    }

    private boolean runSearchableIf(BaseEntity entity, EntityDescr descr) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("entity", entity);
        Boolean value = ScriptingProvider.evaluateGroovy(ScriptingProvider.Layer.CORE, descr.getSearchableIfScript(), params);
        return BooleanUtils.isTrue(value);
    }

    public int processQueue() {
        log.debug("Start processing queue");
        int count = 0;
        boolean locked = writeLock.tryLock();
        if (!locked) {
            log.warn("Unable to process queue: writing at the moment");
            return count;
        }
        try {
            writing = true;

            loginOnce();

            int maxSize = config.getIndexingBatchSize();
            List<FtsQueue> list;

            Transaction tx = Locator.createTransaction();
            try {
                EntityManager em = PersistenceProvider.getEntityManager();
                Query query = em.createQuery("select q from core$FtsQueue q order by q.createTs");
                query.setMaxResults(maxSize);
                list = query.getResultList();
                tx.commit();
            } finally {
                tx.end();
            }

            if (!list.isEmpty()) {
                LuceneIndexer indexer = new LuceneIndexer(getDescrByName(), getDirectory());
                try {
                    for (FtsQueue ftsQueue : list) {
                        indexer.indexEntity(ftsQueue.getEntityName(), ftsQueue.getEntityId(), ftsQueue.getChangeType());
                        count++;
                    }

                    int period = config.getOptimizationPeriod();
                    if (++writeCount > period) {
                        indexer.optimize();
                        writeCount = 0;
                    }
                } finally {
                    indexer.close();
                }

                tx = Locator.createTransaction();
                try {
                    EntityManager em = PersistenceProvider.getEntityManager();

                    for (int i = 0; i < list.size(); i += DEL_CHUNK) {
                        StringBuilder sb = new StringBuilder("delete from SYS_FTS_QUEUE where ID in (");
                        List<FtsQueue> sublist = list.subList(i, Math.min(i + DEL_CHUNK, list.size()));
                        for (int idx = 0; idx < sublist.size(); idx++) {
                            sb.append("?");
                            if (idx < sublist.size() - 1)
                                sb.append(", ");
                        }
                        sb.append(")");

                        Query query = em.createNativeQuery(sb.toString());
                        for (int idx = 0; idx < sublist.size(); idx++) {
                            query.setParameter(idx + 1, sublist.get(idx).getId());
                        }
                        query.executeUpdate();
                    }

                    tx.commit();
                } finally {
                    tx.end();
                }
            }
        } catch (LoginException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
            writing = false;
        }
        log.debug(count + " queue items succesfully processed");
        return count;
    }

    public boolean showInResults(String entityName) {
        EntityDescr descr = getDescrByName().get(entityName);
        return descr != null && descr.isShow();
    }

    public String jmxProcessQueue() {
        try {
            // login performed inside processQueue()
            int count = processQueue();
            return String.format("Done %d items", count);
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    public String jmxOptimize() {
        boolean locked = writeLock.tryLock();
        if (!locked) {
            return "Unable to optimize: writing at the moment";
        }
        try {
            writing = true;
            loginOnce();

            LuceneWriter luceneWriter = new LuceneWriter(getDirectory());
            luceneWriter.optimize();
            luceneWriter.close();

            return "Done";
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        } finally {
            writeLock.unlock();
            writing = false;
        }
    }

    public String jmxReindexEntity(String entityName) {
        try {
            loginOnce();
            deleteIndexForEntity(entityName);
            int count = reindexEntity(entityName);
            return String.format("Enqueued %d items. Reindexing will be performed on next processQueue invocation.", count);
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    private void deleteIndexForEntity(String entityName) {
        boolean locked = writeLock.tryLock();
        if (!locked) {
            throw new IllegalStateException("Unable to delete index: writing at the moment");
        }
        try {
            writing = true;
            LuceneWriter.deleteIndexForEntity(getDirectory(), entityName);
        } finally {
            writeLock.unlock();
            writing = false;
        }
    }

    private void deleteIndex() {
        boolean locked = writeLock.tryLock();
        if (!locked) {
            throw new IllegalStateException("Unable to delete index: writing at the moment");
        }
        try {
            writing = true;

            LuceneWriter writer = new LuceneWriter(getDirectory());
            writer.deleteAll();
            writer.close();
        } finally {
            writeLock.unlock();
            writing = false;
        }
    }

    private int reindexEntity(String entityName) {
        int count = 0;

        MetaClass metaClass = MetadataProvider.getSession().getClass(entityName);
        if (metaClass == null)
            throw new IllegalArgumentException("MetaClass not found for " + entityName);

        Transaction tx = Locator.createTransaction();
        try {
            FtsSender sender = Locator.lookup(FtsSender.NAME);

            sender.emptyQueue(entityName);
            tx.commitRetaining();

            EntityDescr descr = getDescrByName().get(entityName);
            if (descr == null)
                return count;

            EntityManager em = PersistenceProvider.getEntityManager();

            if (StringUtils.isBlank(descr.getSearchableIfScript())) {
                Query q = em.createQuery("select e.id from " + entityName + " e");
                List<UUID> list = q.getResultList();
                for (UUID id : list) {
                    sender.enqueue(entityName, id, FtsChangeType.INSERT);
                    count++;
                }
            } else {
                Query q = em.createQuery("select e from " + entityName + " e");
                List<BaseEntity> list = q.getResultList();
                for (BaseEntity entity : list) {
                    if (runSearchableIf(entity, descr)) {
                        sender.enqueue(entityName, (UUID) entity.getId(), FtsChangeType.INSERT);
                        count++;
                    }
                }
            }
            tx.commit();
        } finally {
            tx.end();
        }
        return count;
    }

    public String jmxReindexAll() {
        try {
            loginOnce();
            deleteIndex();

            int count = 0;
            for (String entityName : getDescrByName().keySet()) {
                count += reindexEntity(entityName);
            }

            return String.format("Enqueued %d items. Reindexing will be performed on next processQueue invocation.", count);
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    public Directory getDirectory() {
        if (directory == null) {
            synchronized (this) {
                if (directory == null) {
                    String dir = config.getIndexDir();
                    if (StringUtils.isBlank(dir))
                        dir = ConfigProvider.getConfig(GlobalConfig.class).getDataDir() + "/ftsindex";
                    File file = new File(dir);
                    if (!file.exists()) {
                        boolean b = file.mkdirs();
                        if (!b)
                            throw new RuntimeException("Directory " + dir + " doesn't exist and can not be created");
                    }
                    try {
                        directory = FSDirectory.open(file);

                        if (directory.fileExists("write.lock")) {
                            directory.deleteFile("write.lock");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return directory;
    }
}
