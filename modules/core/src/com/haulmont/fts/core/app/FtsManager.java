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
package com.haulmont.fts.core.app;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
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
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.core.global.Stores;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.persistence.DbTypeConverter;
import com.haulmont.cuba.security.app.Authenticated;
import com.haulmont.cuba.security.app.Authentication;
import com.haulmont.fts.core.sys.*;
import com.haulmont.fts.global.FtsConfig;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Component(FtsManagerAPI.NAME)
public class FtsManager implements FtsManagerAPI {

    private static final Logger log = LoggerFactory.getLogger(FtsManager.class);

    protected final ReentrantLock writeLock = new ReentrantLock();
    protected volatile boolean writing;

    protected final ReentrantLock reindexLock = new ReentrantLock();
    protected volatile boolean reindexing;
    protected volatile Queue<String> reindexEntitiesQueue = new ConcurrentLinkedQueue<>();

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
    protected FtsSender ftsSender;

    @Inject
    protected LuceneIndexer luceneIndexer;

    @Inject
    protected IndexWriterProvider indexWriterProvider;

    @Inject
    protected IndexSearcherProvider indexSearcherProvider;

    @Inject
    protected EntityDescrsManager entityDescrsManager;

    @Inject
    protected LuceneIndexMaintenance luceneIndexMaintenance;

    @Inject
    protected DirectoryProvider directoryProvider;

    protected static final Pattern TOO_OLD_INDEX_PATTERN = Pattern.compile("this index is too old \\(version: (.+)\\)");

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

    @Override
    public List<Entity> getSearchableEntities(Entity entity) {
        List<Entity> list = new ArrayList<>();

        EntityDescr descr = entityDescrsManager.getDescrByEntityName(entity.getMetaClass().getName());
        if (descr == null) {
            MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(entity.getMetaClass());
            if (originalMetaClass != null)
                descr = entityDescrsManager.getDescrByEntityName(originalMetaClass.getName());
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
        if (!isApplicationContextStarted())
            return 0;

        if (!isEnabled())
            return 0;

        if (!reindexEntitiesQueue.isEmpty()) {
            log.info("Unable to process queue: there are entities that are waiting for reindex");
            return 0;
        }

        log.debug("Start processing queue");
        int count = 0;
        boolean locked = writeLock.tryLock();
        if (!locked) {
            log.info("Unable to process queue: writing at the moment");
            return count;
        }

        authentication.begin();
        try {
            writing = true;

            List<FtsQueue> list = loadQueuedItems();
            list = new ArrayList<>(list);
            if (!list.isEmpty()) {
                count = indexFtsQueueItems(list);
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

    protected boolean isApplicationContextStarted() {
        return AppContext.isStarted();
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

    protected int indexFtsQueueItems(List<FtsQueue> list) {
        List<FtsQueue> notIndexed = new ArrayList<>(list.size());
        int count = 0;
        IndexWriter indexWriter = indexWriterProvider.getIndexWriter();
        try {
            for (FtsQueue ftsQueue : list) {
                try {
                    luceneIndexer.indexEntity(ftsQueue.getEntityName(), ftsQueue.getObjectEntityId(), ftsQueue.getChangeType(), indexWriter);
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
            try {
                indexWriter.commit();
                indexSearcherProvider.getSearcherManager().maybeRefresh();
            } catch (IOException e) {
                throw new RuntimeException("Error on index writer commit", e);
            }
        }
        return count;
    }

    @Override
    public String optimize() {
        return luceneIndexMaintenance.optimize();
    }

    @Override
    public String upgrade() {
        return luceneIndexMaintenance.upgrade();
    }

    @Override
    public boolean showInResults(String entityName) {
        EntityDescr descr = entityDescrsManager.getDescrByEntityName(entityName);
        return descr != null && descr.isShow();
    }

    @Override
    public void deleteIndexForEntity(String entityName) {
        luceneIndexer.deleteDocumentsForEntity(entityName);
    }

    @Override
    public void deleteIndex() {
        luceneIndexer.deleteAllDocuments();
    }

    @Override
    public int reindexEntity(String entityName) {
        MetaClass metaClass = metadata.getClassNN(entityName);
        EntityDescr entityDescr = entityDescrsManager.getDescrByEntityName(metaClass.getName());

        if (entityDescr == null) {
            return 0;
        }

        persistence.runInTransaction(em -> ftsSender.emptyQueue(entityName));

        return persistence.callInTransaction(em -> {
            int count = 0;

            EntitiesCollector collector = AppBeans.getPrototype(EntitiesCollector.NAME, metaClass);
            if (Strings.isNullOrEmpty(entityDescr.getSearchableIfScript())) {
                collector.setIdOnly(true);

                List<Object> ids = executeInStoreTx(metaClass, collector::loadResults);

                for (Object id : ids) {
                    ftsSender.enqueue(metaClass.getName(), id, FtsChangeType.INSERT);
                    count++;
                }

                log.debug("{} instances of {} was added to the FTS queue", count, entityName);
            } else {
                List<Object> ids = executeInStoreTx(metaClass,
                        () -> collector.loadResults().stream()
                                .filter(e -> runSearchableIf((Entity<?>) e, entityDescr))
                                .map(e -> ((Entity<?>) e).getId())
                                .collect(Collectors.toList()));

                for (Object id : ids) {
                    ftsSender.enqueue(metaClass.getName(), id, FtsChangeType.INSERT);
                    count++;
                }

                log.debug("{} instances of {} was processed. {} of them was added to the FTS queue",
                        ids.size(), metaClass.getName(), count);
            }
            return count;
        });
    }

    @Override
    public void asyncReindexEntity(String entityName) {
        metadata.getSession().getClassNN(entityName);
        Preconditions.checkNotNullArgument(entityDescrsManager.getDescrByEntityName(entityName), "FTS configuration not found for %s", entityName);
        persistence.runInTransaction(em -> {
            ftsSender.emptyQueue(entityName);
            reindexEntitiesQueue.add(entityName);
        });
    }

    @Override
    public int reindexAll() {
        int count = 0;
        for (String entityName : entityDescrsManager.getDescrByNameMap().keySet()) {
            count += reindexEntity(entityName);
        }
        return count;
    }

    @Override
    public void asyncReindexAll() {
        entityDescrsManager.getDescrByNameMap().keySet().forEach(this::asyncReindexEntity);
    }

    @Override
    public int reindexNextBatch() {
        if (!isApplicationContextStarted()) {
            return 0;
        }

        if (!ftsConfig.getEnabled()) {
            return 0;
        }

        if (reindexEntitiesQueue.isEmpty()) {
            return 0;
        }

        log.debug("Start reindexing next entities batch");
        boolean locked = reindexLock.tryLock();
        if (!locked) {
            log.info("Unable to reindex next batch of entities: reindexing at the moment");
            return 0;
        }
        try {
            authentication.begin();
            reindexing = true;

            int count = 0;

            MetaClass metaClass = metadata.getClassNN(reindexEntitiesQueue.element());
            EntityDescr entityDescr = entityDescrsManager.getDescrByEntityName(metaClass.getName());

            if (entityDescr == null) {
                return 0;
            }

            EntitiesCollector collector = AppBeans.getPrototype(EntitiesCollector.NAME, metaClass);
            collector.setExcludeFromQueue(true);

            if (Strings.isNullOrEmpty(entityDescr.getSearchableIfScript())) {
                collector.setIdOnly(true);
                List<Object> ids = executeInStoreTx(metaClass, collector::loadResults);

                for (Object id : ids) {
                    ftsSender.enqueue(entityDescr.getMetaClass().getName(), id, FtsChangeType.INSERT);
                    count++;
                }
                if (ids.size() < ftsConfig.getReindexBatchSize()) {
                    reindexEntitiesQueue.remove();
                }
                log.debug("{} instances of {} was added to the FTS queue", count, metaClass.getName());
            } else {
                List<FtsQueue> currentFakeQueueItems = new ArrayList<>();
                List<Object> ids = new ArrayList<>();

                executeInStoreTx(metaClass, () -> {
                    for (Object obj : collector.loadResults()) {
                        Entity<?> entity = (Entity<?>) obj;
                        if (runSearchableIf(entity, entityDescr)) {
                            ids.add(entity.getId());
                        } else {
                            currentFakeQueueItems.add(createFakeFtsQueue(metaClass.getName(), entity.getId()));
                        }
                    }
                    return null;
                });

                for (Object id : ids) {
                    ftsSender.enqueue(metaClass.getName(), id, FtsChangeType.INSERT);
                    count++;
                }

                if (ids.size() < ftsConfig.getReindexBatchSize()) {
                    reindexEntitiesQueue.remove();
                    ftsSender.emptyFakeQueue(metaClass.getName());
                } else {
                    currentFakeQueueItems.forEach(q -> persistence.getEntityManager().persist(q));
                }
                log.debug("{} instances of {} was processed. {} of them was added to the FTS queue",
                        ids.size(), metaClass.getName(), count);
            }

            return count;
        } finally {
            reindexLock.unlock();
            reindexing = false;
            authentication.end();
        }
    }

    protected FtsQueue createFakeFtsQueue(String entityName, Object entityId) {
        FtsQueue q = metadata.create(FtsQueue.class);
        q.setObjectEntityId(entityId);
        q.setEntityName(entityName);
        q.setFake(true);
        return q;
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

    @Override
    public String getIndexFormatVersion() {
        try {
            try {
                SegmentInfos segmentInfos = SegmentInfos.readLatestCommit(directoryProvider.getDirectory());
                Version version = segmentInfos.getMinSegmentLuceneVersion();
                return version != null ? version.toString() : null;
            } catch (IndexFormatTooOldException e) {
                if (e.getVersion() != null) {
                    switch (e.getVersion()) {
                        case SegmentInfos.VERSION_70:
                            return Version.LUCENE_7_0_0.toString();
                        case SegmentInfos.VERSION_72:
                            return Version.LUCENE_7_2_0.toString();
                        case SegmentInfos.VERSION_74:
                            return Version.LUCENE_7_4_0.toString();
                        default:
                            return null;
                    }
                }
                if (e.getReason() != null) {
                    Matcher matcher = TOO_OLD_INDEX_PATTERN.matcher(e.getReason());
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Error while reading index", e);
        }
    }

    @Override
    public String getLatestIndexFormatVersion() {
        return Version.LATEST.toString();
    }

    protected <T> T executeInStoreTx(MetaClass metaClass, Supplier<T> supplier) {
        String storeName = getStoreName(metaClass);
        if (Stores.isMain(storeName)) {
            return supplier.get();
        } else {
            return persistence.callInTransaction(storeName, em -> supplier.get());
        }
    }

    protected String getStoreName(MetaClass metaClass) {
        String storeName = metadata.getTools().getStoreName(metaClass);
        Preconditions.checkNotNullArgument(storeName, "Storage not found for %s", metaClass.getName());
        return storeName;
    }
}