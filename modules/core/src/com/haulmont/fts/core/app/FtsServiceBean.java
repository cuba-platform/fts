/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.core.sys.EntityDescr;
import com.haulmont.fts.core.sys.EntityInfo;
import com.haulmont.fts.core.sys.LuceneSearcher;
import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.SearchResult;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
@Service(FtsService.NAME)
public class FtsServiceBean implements FtsService {

    protected LuceneSearcher searcher;

    @Inject
    protected FtsManagerAPI manager;

    @Inject
    protected PersistenceSecurity security;

    @Inject
    protected Persistence persistence;

    @Inject
    protected Metadata metadata;

    @Inject
    protected Messages messages;

    protected FtsConfig config;

    @Inject
    public void setConfigProvider(Configuration configuration) {
        config = configuration.getConfig(FtsConfig.class);
    }

    protected LuceneSearcher getSearcher() {
        if (searcher == null) {
            synchronized (this) {
                if (searcher == null) {
                    searcher = new LuceneSearcher(manager.getDirectory(), config.getStoreContentInIndex());
                }
            }
        }
        return searcher;
    }

    @Override
    public SearchResult search(String searchTerm) {
        if (searcher != null && !searcher.isCurrent())
            searcher = null;

        int maxResults = config.getMaxSearchResults();
        List<EntityInfo> allFieldResults = getSearcher().searchAllField(searchTerm, maxResults);

        return makeSearchResult(searchTerm, maxResults, allFieldResults);
    }

    @Override
    public SearchResult search(String searchTerm, List<String> entityNames) {
        if (searcher != null && !searcher.isCurrent())
            searcher = null;

        //first search among entities with names from entityNames method parameter
        List<EntityInfo> allFieldResults = getSearcher().searchAllField(searchTerm, entityNames);

        SearchResult searchResult = new SearchResult(searchTerm);
        for (EntityInfo entityInfo : allFieldResults) {
            searchResult.addHit(entityInfo.getId(), entityInfo.getText(), null, new MorphologyNormalizer());
            //we don't reload entity because we don't need entity caption
            SearchResult.Entry entry = new SearchResult.Entry(entityInfo.getId(), entityInfo.getId().toString());
            searchResult.addEntry(entityInfo.getName(), entry);
        }

        //try to find entities that has a link to other entities (not from entityNames collection)
        //that matches a search criteria
        Set<String> linkedEntitiesNames = new HashSet<>();
        for (String entityName : entityNames) {
            linkedEntitiesNames.addAll(findLinkedEntitiesNames(entityName));
        }

        List<EntityInfo> linkedEntitiesInfos = getSearcher().searchAllField(searchTerm, linkedEntitiesNames);
        for (EntityInfo linkedEntitiesInfo : linkedEntitiesInfos) {
            List<EntityInfo> entitiesWithLinkInfos = getSearcher().searchLinksField(linkedEntitiesInfo.getId(), entityNames);
            for (EntityInfo entityWithLinkInfo : entitiesWithLinkInfos) {
                searchResult.addHit(entityWithLinkInfo.getId(), linkedEntitiesInfo.getText(), linkedEntitiesInfo.getName(),
                        new MorphologyNormalizer());
                searchResult.addEntry(entityWithLinkInfo.getName(), new SearchResult.Entry(entityWithLinkInfo.getId(), entityWithLinkInfo.getId().toString()));
            }
        }

        return searchResult;
    }

    /**
     * Iterates through entity indexed link properties and returns a collection of
     * these properties entity names
     */
    protected Set<String> findLinkedEntitiesNames(String entityName) {
        Set<String> result = new HashSet<>();
        EntityDescr entityDescr = manager.getDescrByName().get(entityName);
        if (entityDescr == null)
            return result;
        List<String> linkProperties = entityDescr.getLinkProperties();
        MetaClass metaClass = metadata.getClass(entityName);
        if (metaClass == null) {
            throw new RuntimeException("Entity with name " + entityName + " not found");
        }
        for (String linkProperty : linkProperties) {
            MetaPropertyPath propertyPath = metaClass.getPropertyPath(linkProperty);
            if (propertyPath == null) {
                throw new RuntimeException("Property path " + linkProperty + " for entity " + entityName + " doesn't exist");
            }
            String linkedEntityName = propertyPath.getMetaProperty().getRange().asClass().getName();
            List<String> collectedLinkedEntityNames = collectEntityHierarchyNames(linkedEntityName);
            result.addAll(collectedLinkedEntityNames);
        }
        return result;
    }

    protected SearchResult makeSearchResult(String searchTerm, int maxResults, List<EntityInfo> allFieldResults) {
        SearchResult result = new SearchResult(searchTerm);

        if (!allFieldResults.isEmpty()) {
            Transaction tx = persistence.createTransaction();
            try {
                for (EntityInfo entityInfo : allFieldResults) {
                    if (!manager.showInResults(entityInfo.getName()))
                        continue;

                    if (result.getEntriesCount(entityInfo.getName()) < config.getSearchResultsBatchSize()) {
                        SearchResult.Entry entry = createEntry(entityInfo.getName(), entityInfo.getId());
                        if (entry != null) {
                            result.addEntry(entityInfo.getName(), entry);
                        }
                    } else {
                        if (!result.hasEntry(entityInfo.getName(), entityInfo.getId())) {
                            result.addId(entityInfo.getName(), entityInfo.getId());
                        }
                    }
                    result.addHit(entityInfo.getId(), entityInfo.getText(), null,
                            new MorphologyNormalizer());
                }
                tx.commit();
            } finally {
                tx.end();
            }

            for (EntityInfo entityInfo : allFieldResults) {
                List<EntityInfo> linksFieldResults = getSearcher().searchLinksField(entityInfo.getId(), maxResults);
                if (!linksFieldResults.isEmpty()) {
                    tx = persistence.createTransaction();
                    try {
                        for (EntityInfo linkEntityInfo : linksFieldResults) {
                            if (!manager.showInResults(linkEntityInfo.getName()))
                                continue;

                            if (result.getEntriesCount(linkEntityInfo.getName()) < config.getSearchResultsBatchSize()) {
                                SearchResult.Entry entry = createEntry(linkEntityInfo.getName(), linkEntityInfo.getId());
                                if (entry != null) {
                                    result.addEntry(linkEntityInfo.getName(), entry);
                                }
                            } else {
                                if (!result.hasEntry(linkEntityInfo.getName(), linkEntityInfo.getId())) {
                                    result.addId(linkEntityInfo.getName(), linkEntityInfo.getId());
                                }
                            }
                            result.addHit(linkEntityInfo.getId(), entityInfo.getText(), entityInfo.getName(),
                                    new MorphologyNormalizer());
                        }
                        tx.commit();
                    } finally {
                        tx.end();
                    }
                }
            }
        }
        return result;
    }

    @Override
    public SearchResult expandResult(SearchResult result, String entityName) {
        int max = result.getEntriesCount(entityName) + config.getSearchResultsBatchSize();

        Transaction tx = persistence.createTransaction();
        try {
            for (UUID id : result.getIds(entityName)) {
                if (result.getEntriesCount(entityName) >= max)
                    break;

                SearchResult.Entry entry = createEntry(entityName, id);
                if (entry != null) {
                    result.addEntry(entityName, entry);
                }

                result.removeId(entityName, id);
            }
            tx.commit();
        } finally {
            tx.end();
        }
        return result;
    }

    @Override
    public boolean isEntityIndexed(String entityName) {
        return manager.showInResults(entityName);
    }

    protected SearchResult.Entry createEntry(String entityName, UUID entityId) {
        MetaClass metaClass = metadata.getSession().getClassNN(entityName);

        if (!security.isEntityOpPermitted(metaClass, EntityOp.READ))
            return null;

        Entity entity = getReloadedEntity(entityName, entityId, metaClass);
        if (entity == null)
            return null;

        String entityCaption = entity.getInstanceName();
        return new SearchResult.Entry(entityId, entityCaption);
    }

    protected Entity getReloadedEntity(String entityName, UUID entityId, MetaClass metaClass) {
        EntityManager em = persistence.getEntityManager();

        Query query = em.createQuery("select e from " + entityName + " e where e.id = :id");
        security.applyConstraints(query);

        query.setParameter("id", entityId);

        query.setView(metadata.getViewRepository().getView(metaClass.getJavaClass(), View.MINIMAL));

        List<Entity> list = query.getResultList();
        if (list.isEmpty())
            return null;
        return list.get(0);
    }

    @Override
    public List<String> collectEntityHierarchyNames(String entityName) {
        MetaClass metaClass = metadata.getClass(entityName);
        if (metaClass == null)
            throw new IllegalArgumentException("Entity with name " + entityName + " does not exist");

        List<String> result = new ArrayList<>();
        result.add(entityName);

        for (MetaClass descendantMetaClass : metaClass.getDescendants()) {
            result.add(descendantMetaClass.getName());
        }

        MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(metaClass);
        if (originalMetaClass != null) {
            result.add(originalMetaClass.getName());
        }

        return result;
    }

    @Override
    public String getHitPropertyCaption(String entityName, String hitProperty) {
        String[] parts = hitProperty.split("\\.");
        if (parts.length == 1) {
            MetaClass metaClass = metadata.getSession().getClass(entityName);
            if (metaClass == null)
                return hitProperty;

            MetaProperty metaProperty = metaClass.getProperty(hitProperty);
            if (metaProperty == null)
                return hitProperty;

            return messages.getTools().getPropertyCaption(metaProperty);
        } else {
            String linkEntityName = parts[0];
            MetaClass metaClass = metadata.getSession().getClass(linkEntityName);
            if (metaClass == null)
                return hitProperty;

            MetaClass fileMetaClass = metadata.getSession().getClassNN(FileDescriptor.class);

            if (metaClass == fileMetaClass && parts[1].equals(FTS.FILE_CONT_PROP)) {
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < parts.length; i++) {
                    sb.append(parts[i]);
                    if (i < parts.length - 1)
                        sb.append(".");
                }
                return messages.formatMessage(FtsServiceBean.class, "fileContent", sb.toString());
            }

            MetaProperty metaProperty = metaClass.getProperty(parts[1]);
            if (metaProperty == null)
                return hitProperty;

            return messages.getTools().getEntityCaption(metaClass) + "."
                    + messages.getTools().getPropertyCaption(metaProperty);
        }
    }
}