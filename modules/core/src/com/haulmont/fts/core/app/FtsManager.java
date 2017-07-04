/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.app;

import com.google.common.base.Strings;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.app.FtsSender;
import com.haulmont.cuba.core.app.ServerInfoAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FtsChangeType;
import com.haulmont.cuba.core.entity.FtsQueue;
import com.haulmont.cuba.core.entity.HasUuid;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.persistence.DbTypeConverter;
import com.haulmont.cuba.security.app.Authenticated;
import com.haulmont.cuba.security.app.Authentication;
import com.haulmont.fts.core.sys.*;
import com.haulmont.fts.global.FtsConfig;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.IndexUpgrader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.EmbeddedId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static java.lang.String.format;

@Component(FtsManagerAPI.NAME)
public class FtsManager implements FtsManagerAPI {

    private final Logger log = LoggerFactory.getLogger(FtsManager.class);

    private volatile Map<String, EntityDescr> descrByClassName;
    private volatile Map<String, EntityDescr> descrByName;

    protected final ReentrantLock writeLock = new ReentrantLock();
    protected volatile boolean writing;

    protected final ReentrantLock reindexLock = new ReentrantLock();
    protected volatile boolean reindexing;
    protected volatile Queue<String> reindexEntitiesQueue = new ConcurrentLinkedQueue<>();

    protected volatile Directory directory;

    protected static final int DEL_CHUNK = 10;

    @Inject
    protected FtsConfig ftsConfig;

    protected String serverId;

    @Inject
    protected Authentication authentication;

    @Inject
    protected Persistence persistence;

    @Inject
    protected Scripting scripting;

    @Inject
    protected Metadata metadata;

    @Inject
    protected ConfigLoader configLoader;

    @Inject
    protected FtsSender ftsSender;

    @Inject
    public void setServerInfo(ServerInfoAPI serverInfo) {
        serverId = serverInfo.getServerId();
    }

    @Override
    public boolean isEnabled() {
        try {
            return ftsConfig.getEnabled();
        } catch (Exception e) {
            log.error("Unable to find out if FTS is enabled: {}", e.toString());
            return false;
        }
    }

    @Authenticated
    @Override
    public void setEnabled(boolean value) {
        ftsConfig.setEnabled(value);
    }

    @Override
    public boolean isWriting() {
        return writing;
    }

    @Override
    public boolean isReindexing() {
        return reindexing;
    }

    @Override
    public Queue<String> getReindexEntitiesQueue() {
        return reindexEntitiesQueue;
    }

    protected Map<String, EntityDescr> getDescrByClassName() {
        if (descrByClassName == null) {
            synchronized (this) {
                if (descrByClassName == null) {
                    descrByClassName = configLoader.loadConfiguration();
                }
            }
        }
        return descrByClassName;
    }

    @Override
    public Map<String, EntityDescr> getDescrByName() {
        if (descrByName == null) {
            synchronized (this) {
                if (descrByName == null) {
                    descrByName = new HashMap<>(getDescrByClassName().size());
                    for (EntityDescr descr : getDescrByClassName().values()) {
                        String name = descr.getMetaClass().getName();
                        descrByName.put(name, descr);
                    }
                }
            }
        }
        return descrByName;
    }

    @Override
    public List<Entity> getSearchableEntities(Entity entity) {
        List<Entity> list = new ArrayList<>();

        EntityDescr descr = getDescrByClassName().get(entity.getClass().getName());
        if (descr == null) {
            Class originalClass = metadata.getExtendedEntities().getOriginalClass(entity.getMetaClass());
            if (originalClass != null)
                descr = getDescrByClassName().get(originalClass.getName());
            if (descr == null)
                return list;
        }

        Set<String> properties = descr.getPropertyNames();

        Set<String> ownProperties = new HashSet<>(properties.size());
        for (String property : properties) {
            String p = property.indexOf(".") < 0 ? property : property.substring(0, property.indexOf("."));
            ownProperties.add(p);
        }

        Set<String> dirty = persistence.getTools().getDirtyFields(entity);
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
            Map<String, Object> params = new HashMap<>();
            params.put("entity", entity);
            params.put("searchables", list);
            scripting.evaluateGroovy(descr.getSearchablesScript(), params);
        }

        return list;
    }

    protected boolean runSearchableIf(Entity entity, EntityDescr descr) {
        Map<String, Object> params = new HashMap<>();
        params.put("entity", entity);
        Boolean value = scripting.evaluateGroovy(descr.getSearchableIfScript(), params);
        return BooleanUtils.isTrue(value);
    }

    @Override
    public int processQueue() {
        if (!AppContext.isStarted())
            return 0;

        if (!isEnabled())
            return 0;

        if (!reindexEntitiesQueue.isEmpty()) {
            log.warn("Unable to process queue: there are entities that are waiting for reindex");
            return 0;
        }

        log.debug("Start processing queue");
        int count = 0;
        boolean locked = writeLock.tryLock();
        if (!locked) {
            log.warn("Unable to process queue: writing at the moment");
            return count;
        }

        authentication.begin();
        try {
            writing = true;

            List<FtsQueue> list = loadQueuedItems();
            list = new ArrayList<>(list);
            if (!list.isEmpty()) {
                count = initIndexer(list);
                removeQueuedItems(list);
            }
        } finally {
            writeLock.unlock();
            writing = false;
            authentication.end();
        }
        log.debug("{} queue items successfully processed", count);
        return count;
    }

    protected List<FtsQueue> loadQueuedItems() {
        boolean useServerId = !ftsConfig.getIndexingHosts().isEmpty();
        int maxSize = ftsConfig.getIndexingBatchSize();
        return persistence.callInTransaction(em -> {
            String queryString = format("select q from sys$FtsQueue q where q.fake = false and %s order by q.createTs",
                    (useServerId ? "q.indexingHost = ?1" : "q.indexingHost is null"));
            TypedQuery<FtsQueue> query = em.createQuery(queryString, FtsQueue.class);
            if (useServerId)
                query.setParameter(1, serverId);
            query.setMaxResults(maxSize);
            return query.getResultList();
        });
    }

    protected void removeQueuedItems(List<FtsQueue> list) {
        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            for (int i = 0; i < list.size(); i += DEL_CHUNK) {
                StringBuilder sb = new StringBuilder("delete from SYS_FTS_QUEUE where ID in (");
                List<FtsQueue> sublist = list.subList(i, Math.min(i + DEL_CHUNK, list.size()));
                for (int idx = 0; idx < sublist.size(); idx++) {
                    sb.append("?");
                    if (idx < sublist.size() - 1)
                        sb.append(", ");
                }
                sb.append(")");

                DbTypeConverter converter = persistence.getDbTypeConverter();

                Query query = em.createNativeQuery(sb.toString());
                for (int idx = 0; idx < sublist.size(); idx++) {
                    query.setParameter(idx + 1, converter.getSqlObject(sublist.get(idx).getId()));
                }
                query.executeUpdate();
            }

            tx.commit();
        }
    }

    protected int initIndexer(List<FtsQueue> list) {
        LuceneIndexerAPI indexer = createLuceneIndexer();
        List<FtsQueue> notIndexed = new ArrayList<>(list.size());
        int count = 0;
        try {
            for (FtsQueue ftsQueue : list) {
                try {
                    indexer.indexEntity(ftsQueue.getEntityName(), ftsQueue.getObjectEntityId(), ftsQueue.getChangeType());
                    count++;
                } catch (IndexingException e) {
                    if (e.getEntityType() != IndexingException.EntityType.FILE)
                        notIndexed.add(ftsQueue);
                }
            }
            if (!notIndexed.isEmpty()) {
                list.removeAll(notIndexed);
            }
        } finally {
            indexer.close();
        }
        return count;
    }

    protected LuceneIndexerAPI createLuceneIndexer() {
        return AppBeans.getPrototype(LuceneIndexerAPI.NAME, getDescrByName(), getDirectory(), ftsConfig.getStoreContentInIndex());
    }

    @Override
    public String optimize() {
        if (!AppContext.isStarted())
            return "Application is not started";

        if (!ftsConfig.getEnabled())
            return "FTS is disabled";

        log.debug("Start optimize");
        boolean locked = writeLock.tryLock();
        if (!locked) {
            return "Unable to optimize: writing at the moment";
        }

        authentication.begin();
        LuceneWriter luceneWriter = new LuceneWriter(getDirectory());
        try {
            writing = true;
            luceneWriter.optimize();
            return "Done";
        } catch (Throwable e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        } finally {
            luceneWriter.close();
            writeLock.unlock();
            writing = false;
            authentication.end();
        }
    }

    @Override
    public String upgrade() {
        IndexUpgrader upgrader = new IndexUpgrader(getDirectory());
        try {
            upgrader.upgrade();
        } catch (IOException e) {
            log.error("Error", e);
            return ExceptionUtils.getStackTrace(e);
        }
        return "successful";
    }

    @Override
    public boolean showInResults(String entityName) {
        EntityDescr descr = getDescrByName().get(entityName);
        return descr != null && descr.isShow();
    }

    @Override
    public void deleteIndexForEntity(String entityName) {
        boolean locked = writeLock.tryLock();
        if (!locked) {
            throw new IllegalStateException("Unable to delete index: writing at the moment");
        }
        LuceneWriter writer = new LuceneWriter(getDirectory());
        try {
            writing = true;
            writer.deleteIndexForEntity(entityName);
        } finally {
            writer.close();
            writeLock.unlock();
            writing = false;
        }
    }

    @Override
    public void deleteIndex() {
        boolean locked = writeLock.tryLock();
        if (!locked) {
            throw new IllegalStateException("Unable to delete index: writing at the moment");
        }
        LuceneWriter writer = new LuceneWriter(getDirectory());
        try {
            writing = true;
            writer.deleteAll();
        } finally {
            writer.close();
            writeLock.unlock();
            writing = false;
        }
    }

    @Override
    public int reindexEntity(String entityName) {
        persistence.runInTransaction(em -> ftsSender.emptyQueue(entityName));
        return executeReindexInTx(entityName, entityDescr -> {
            int count = 0;
            MetaClass metaClass = entityDescr.getMetaClass();
            EntitiesCollector collector = AppBeans.getPrototype(EntitiesCollector.NAME, metaClass);
            if (Strings.isNullOrEmpty(entityDescr.getSearchableIfScript())) {
                collector.setIdOnly(true);
                for (Object id : collector.loadResults()) {
                    ftsSender.enqueue(metaClass.getName(), id, FtsChangeType.INSERT);
                    count++;
                }
                log.debug("{} instances of {} was added to the FTS queue", count, entityName);
            } else {
                List result = collector.loadResults();
                for (Object obj : result) {
                    Entity entity = (Entity) obj;
                    if (runSearchableIf(entity, entityDescr)) {
                        ftsSender.enqueue(metaClass.getName(), entity.getId(), FtsChangeType.INSERT);
                        count++;
                    }
                }
                log.debug("{} instances of {} was processed. {} of them was added to the FTS queue",
                        result.size(), metaClass.getName(), count);
            }
            return count;
        });
    }

    @Override
    public void asyncReindexEntity(String entityName) {
        metadata.getSession().getClassNN(entityName);
        Preconditions.checkNotNullArgument(getDescrByName().get(entityName), "FTS configuration not found for %s", entityName);
        persistence.runInTransaction(em -> {
            ftsSender.emptyQueue(entityName);
            reindexEntitiesQueue.add(entityName);
        });
    }

    @Override
    public int reindexAll() {
        int count = 0;
        for (String entityName : getDescrByName().keySet()) {
            count += reindexEntity(entityName);
        }
        return count;
    }

    @Override
    public void asyncReindexAll() {
        getDescrByName().keySet().forEach(this::asyncReindexEntity);
    }

    @Override
    public int reindexNextBatch() {
        if (!AppContext.isStarted())
            return 0;

        if (!ftsConfig.getEnabled())
            return 0;

        if (reindexEntitiesQueue.isEmpty())
            return 0;

        log.debug("Start reindexing next entities batch");
        boolean locked = reindexLock.tryLock();
        if (!locked) {
            log.warn("Unable to reindex next batch of entities: reindexing at the moment");
            return 0;
        }
        try {
            authentication.begin();
            reindexing = true;
            return executeReindexInTx(reindexEntitiesQueue.element(), entityDescr -> {
                int count = 0;
                MetaClass metaClass = entityDescr.getMetaClass();
                EntitiesCollector collector = AppBeans.getPrototype(EntitiesCollector.NAME, metaClass);
                collector.setExcludeFromQueue(true);
                if (Strings.isNullOrEmpty(entityDescr.getSearchableIfScript())) {
                    collector.setIdOnly(true);
                    List result = collector.loadResults();
                    for (Object id : result) {
                        ftsSender.enqueue(entityDescr.getMetaClass().getName(), id, FtsChangeType.INSERT);
                        count++;
                    }
                    if (result.size() < ftsConfig.getReindexBatchSize()) {
                        reindexEntitiesQueue.remove();
                    }
                    log.debug("{} instances of {} was added to the FTS queue", count, metaClass.getName());
                } else {
                    List result = collector.loadResults();
                    for (Object obj : result) {
                        Entity entity = (Entity) obj;
                        if (runSearchableIf(entity, entityDescr)) {
                            ftsSender.enqueue(metaClass.getName(), entity.getId(), FtsChangeType.INSERT);
                            count++;
                        } else {
                            ftsSender.enqueueFake(metaClass.getName(), entity.getId());
                        }
                    }
                    if (result.size() < ftsConfig.getReindexBatchSize()) {
                        reindexEntitiesQueue.remove();
                        ftsSender.emptyFakeQueue(metaClass.getName());
                    }
                    log.debug("{} instances of {} was processed. {} of them was added to the FTS queue",
                            result.size(), metaClass.getName(), count);
                }
                return count;
            });
        } finally {
            reindexLock.unlock();
            reindexing = false;
            authentication.end();
        }
    }

    protected int executeReindexInTx(String entityName, Function<EntityDescr, Integer> indexAction) {
        MetaClass metaClass = metadata.getSession().getClassNN(entityName);
        String storeName = metadata.getTools().getStoreName(metaClass);
        Preconditions.checkNotNullArgument(storeName, "Storage not found for %s", metaClass.getName());
        EntityDescr entityDescr = getDescrByName().get(metaClass.getName());
        int count = 0;
        if (entityDescr != null) {
            try (Transaction tx = persistence.createTransaction()) {
                if (Stores.isMain(storeName)) {
                    count = indexAction.apply(entityDescr);
                } else {
                    try (Transaction storeTx = persistence.createTransaction(storeName)) {
                        count = indexAction.apply(entityDescr);
                        storeTx.commit();
                    }
                }
                tx.commit();
            }
        }
        return count;
    }

    @Override
    public Directory getDirectory() {
        if (directory == null) {
            synchronized (this) {
                if (directory == null) {
                    String dir = ftsConfig.getIndexDir();
                    if (StringUtils.isBlank(dir)) {
                        Configuration configuration = AppBeans.get(Configuration.NAME);
                        dir = configuration.getConfig(GlobalConfig.class).getDataDir() + "/ftsindex";
                    }
                    Path file = Paths.get(dir);
                    if (!Files.exists(file)) {
                        try {
                            Files.createDirectory(file);
                        } catch (IOException e) {
                            throw new RuntimeException("Directory " + dir + " doesn't exist and can not be created");
                        }
                    }
                    try {
                        directory = FSDirectory.open(file);
                        if (Files.exists(file.resolve("write.lock"))) {
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

    @Override
    public boolean isEntityCanBeIndexed(MetaClass metaClass) {
        return !(metadata.getTools().hasCompositePrimaryKey(metaClass) && !HasUuid.class.isAssignableFrom(metaClass.getJavaClass()));
    }

    @Override
    public MetaProperty getPrimaryKeyPropertyForFts(MetaClass metaClass) {
        if (metadata.getTools().hasCompositePrimaryKey(metaClass) && HasUuid.class.isAssignableFrom(metaClass.getJavaClass())) {
            return metaClass.getPropertyNN("uuid");
        }
        return metadata.getTools().getPrimaryKeyProperty(metaClass);
    }

}