/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.MetadataObject;
import com.haulmont.cuba.core.PersistenceSecurity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.core.sys.EntityDescr;
import com.haulmont.fts.core.sys.EntityDescrsManager;
import com.haulmont.fts.core.sys.EntityInfo;
import com.haulmont.fts.core.sys.LuceneSearcher;
import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.FtsConfig;
import com.haulmont.fts.global.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service(FtsService.NAME)
public class FtsServiceBean implements FtsService {

    @Inject
    protected FtsManagerAPI manager;

    @Inject
    protected PersistenceSecurity security;

    @Inject
    protected Metadata metadata;

    @Inject
    protected Messages messages;

    @Inject
    protected FtsConfig coreConfig;

    @Inject
    protected LuceneSearcher luceneSearcher;

    @Inject
    protected DataManager dataManager;

    private final Logger log = LoggerFactory.getLogger(FtsService.class);

    @Inject
    protected EntityDescrsManager entityDescrsManager;

    @Override
    public SearchResult search(String searchTerm) {
        int maxResults = coreConfig.getMaxSearchResults();
        List<EntityInfo> allFieldResults = luceneSearcher.searchAllField(searchTerm, maxResults);

        return makeSearchResult(searchTerm, maxResults, allFieldResults);
    }

    @Override
    public SearchResult search(String searchTerm, List<String> entityNames) {
        //first search among entities with names from entityNames method parameter
        List<EntityInfo> allFieldResults = luceneSearcher.searchAllField(searchTerm, entityNames);

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

        List<EntityInfo> linkedEntitiesInfos = luceneSearcher.searchAllField(searchTerm, linkedEntitiesNames);
        for (EntityInfo linkedEntitiesInfo : linkedEntitiesInfos) {
            List<EntityInfo> entitiesWithLinkInfos = luceneSearcher.searchLinksField(linkedEntitiesInfo.toString(), entityNames);
            //for backward compatibility. Previously "links" field of the Lucene document contained a set of linked entities ids.
            //Now a set of {@link EntityInfo} objects is stored there. We need to make a second search to find entities,
            //that were indexed before this modification.
            entitiesWithLinkInfos.addAll(luceneSearcher.searchLinksField(linkedEntitiesInfo.getId(), entityNames));
            for (EntityInfo entityWithLinkInfo : entitiesWithLinkInfos) {
                searchResult.addHit(entityWithLinkInfo.getId(), linkedEntitiesInfo.getText(), linkedEntitiesInfo.getName(),
                        new MorphologyNormalizer());
                searchResult.addEntry(entityWithLinkInfo.getName(), new SearchResult.Entry(entityWithLinkInfo.getId(), entityWithLinkInfo.getId().toString()));
            }
        }

        return searchResult;
    }

    /**
     * Iterates through entity indexed link properties and returns a collection of these properties entity names
     */
    protected Set<String> findLinkedEntitiesNames(String entityName) {
        Set<String> result = new HashSet<>();
        EntityDescr entityDescr = entityDescrsManager.getDescrByEntityName(entityName);
        if (entityDescr == null)
            return result;
        List<String> linkProperties = entityDescr.getLinkProperties();
        MetaClass metaClass = metadata.getClassNN(entityName);
        for (String linkProperty : linkProperties) {
            MetaPropertyPath propertyPath = metaClass.getPropertyPath(linkProperty);
            if (propertyPath == null) {
                throw new RuntimeException(String.format("Property path %s for entity %s doesn't exist", linkProperty, entityName));
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
            for (EntityInfo entityInfo : allFieldResults) {
                if (!manager.showInResults(entityInfo.getName()))
                    continue;

                if (result.getEntriesCount(entityInfo.getName()) < coreConfig.getSearchResultsBatchSize()) {
                    MetaClass metaClass = metadata.getSession().getClassNN(entityInfo.getName());
                    SearchResult.Entry entry = createEntry(metaClass, entityInfo.getId());
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

            for (EntityInfo entityInfo : allFieldResults) {
                List<EntityInfo> linksFieldResults = luceneSearcher.searchLinksField(entityInfo.toString(), maxResults);
                //for backward compatibility. Previously "links" field of the Lucene document contained a set of linked entities ids.
                //Now a set of {@link EntityInfo} strings is stored there. We need to make a second search to find entities,
                //that were indexed before this modification.
                linksFieldResults.addAll(luceneSearcher.searchLinksField(entityInfo.getId(), maxResults));
                if (!linksFieldResults.isEmpty()) {
                    for (EntityInfo linkEntityInfo : linksFieldResults) {
                        if (!manager.showInResults(linkEntityInfo.getName()))
                            continue;

                        if (result.getEntriesCount(linkEntityInfo.getName()) < coreConfig.getSearchResultsBatchSize()) {
                            MetaClass metaClass = metadata.getSession().getClassNN(linkEntityInfo.getName());
                            SearchResult.Entry entry = createEntry(metaClass, linkEntityInfo.getId());
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
                }
            }
        }
        return result;
    }

    @Override
    public SearchResult expandResult(SearchResult result, String entityName) {
        int max = result.getEntriesCount(entityName) + coreConfig.getSearchResultsBatchSize();
        for (Object id : result.getIds(entityName)) {
            if (result.getEntriesCount(entityName) >= max)
                break;
            MetaClass metaClass = metadata.getSession().getClassNN(entityName);
            SearchResult.Entry entry = createEntry(metaClass, id);
            if (entry != null) {
                result.addEntry(entityName, entry);
            }

            result.removeId(entityName, id);
        }
        return result;
    }

    @Override
    public boolean isEntityIndexed(String entityName) {
        return manager.showInResults(entityName);
    }

    protected SearchResult.Entry createEntry(MetaClass metaClass, Object entityId) {
        if (!security.isEntityOpPermitted(metaClass, EntityOp.READ))
            return null;

        Entity entity = getReloadedEntity(entityId, metaClass);
        if (entity == null)
            return null;

        String entityCaption = entity.getInstanceName();
        return new SearchResult.Entry(entityId, entityCaption);
    }

    protected Entity getReloadedEntity(Object entityId, MetaClass metaClass) {
        MetaProperty primaryKeyForFts = manager.getPrimaryKeyPropertyForFts(metaClass);
        LoadContext.Query query = LoadContext.createQuery(String.format("select e from %s e where e.%s = :id", metaClass.getName(), primaryKeyForFts.getName()))
                .setParameter("id", entityId);
        LoadContext ctx = new LoadContext(metaClass)
                .setQuery(query)
                .setView(metadata.getViewRepository().getView(metaClass, View.MINIMAL));
        List list = dataManager.secure().loadList(ctx);
        return list.isEmpty() ? null : (Entity) list.get(0);
    }

    @Override
    public List<String> collectEntityHierarchyNames(String entityName) {
        MetaClass metaClass = metadata.getClassNN(entityName);

        List<String> result = new ArrayList<>();
        result.add(entityName);

        result.addAll(metaClass.getDescendants().stream().map(MetadataObject::getName).collect(Collectors.toList()));

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

    @Override
    public MetaProperty getPrimaryKeyPropertyForFts(MetaClass metaClass) {
        return manager.getPrimaryKeyPropertyForFts(metaClass);
    }
}