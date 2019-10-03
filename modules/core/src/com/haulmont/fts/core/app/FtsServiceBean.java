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

import com.google.common.base.Joiner;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.MetadataObject;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.fts.app.FtsSearchOption;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.app.HitInfoLoaderService;
import com.haulmont.fts.core.sys.DatabaseDataLoader;
import com.haulmont.fts.core.sys.EntityDescr;
import com.haulmont.fts.core.sys.EntityDescrsManager;
import com.haulmont.fts.core.sys.LuceneSearcher;
import com.haulmont.fts.global.*;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Service(FtsService.NAME)
public class FtsServiceBean implements FtsService {

    @Inject
    protected FtsManagerAPI manager;

    @Inject
    protected Metadata metadata;

    @Inject
    protected Messages messages;

    @Inject
    protected FtsConfig coreConfig;

    @Inject
    protected LuceneSearcher luceneSearcher;

    @Inject
    protected EntityDescrsManager entityDescrsManager;

    @Inject
    protected DatabaseDataLoader databaseDataLoader;

    @Inject
    protected HitInfoLoaderService hitInfoLoaderService;

    @Override
    public SearchResult search(String searchTerm, QueryKey queryKey, FtsSearchOption... searchOptions) {
        SearchResult searchResult = new SearchResult(searchTerm);

        int maxSearchResults = coreConfig.getMaxSearchResults();

        boolean searchByTerm = false;
        if (queryKey == null || queryKey.isSearchByTermAgain()) {
            searchByTerm = true;
            searchByTerm(searchResult, searchTerm, queryKey, maxSearchResults);
            if (searchResult.getCount() == 0  && searchResult.getIdsCount() == 0) {
                return searchResult;
            }
        }

        if (!searchByTerm) {
            searchResult.getQueryKey().setEntityInfos(queryKey.getEntityInfos());
            searchResult.getQueryKey().setFirstResult(queryKey.getFirstResult());
        }

        searchByLinks(queryKey, searchResult, maxSearchResults);

        if (!searchByTerm && searchResult.getCount() == 0 && searchResult.getQueryKey().isSearchByTermAgain()) {
            searchResult = search(searchTerm, searchResult.getQueryKey(), searchOptions);
        }

        if (coreConfig.getStoreContentInIndex() && ArrayUtils.contains(searchOptions, FtsSearchOption.POPULATE_HIT_INFOS)) {
            hitInfoLoaderService.populateHitInfos(searchResult);
        }

        return searchResult;
    }

    @Override
    public SearchResult search(String searchTerm, List<String> entityNames) {
        //first search among entities with names from entityNames method parameter
        List<EntityInfo> allFieldResults = luceneSearcher.searchAllField(searchTerm, entityNames);

        SearchResult searchResult = new SearchResult(searchTerm);
        for (EntityInfo entityInfo : allFieldResults) {
            //we don't reload entity because we don't need entity caption
            SearchResultEntry searchResultEntry = new SearchResultEntry(entityInfo);
            searchResultEntry.setDirectResult(true);
            searchResult.addEntry(searchResultEntry);
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
                SearchResultEntry entry = searchResult.getEntryByEntityInfo(entityWithLinkInfo);
                if (entry == null) {
                    entry = new SearchResultEntry(entityWithLinkInfo);
                    entry.setDirectResult(false);
                    searchResult.addEntry(entry);
                }
                entry.addLinkedEntity(linkedEntitiesInfo);
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

    @Override
    public boolean isEntityIndexed(String entityName) {
        return manager.showInResults(entityName);
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
        List<String> captionParts = new ArrayList<>();
        String[] parts = hitProperty.split("\\.");
        MetaClass fileMetaClass = metadata.getSession().getClassNN(FileDescriptor.class);
        MetaClass lastMetaClass = metadata.getSession().getClass(entityName);
        for (int i = 0; i < parts.length; i++) {
            MetaProperty lastMetaProperty = lastMetaClass.getProperty(parts[i]);

            if (lastMetaClass == fileMetaClass && FTS.FILE_CONT_PROP.equals(parts[i])) {
                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < parts.length; j++) {
                    sb.append(parts[j]);
                    if (j < parts.length - 1)
                        sb.append(".");
                }
                captionParts.add(messages.formatMessage(FtsServiceBean.class, "fileContent", sb.toString()));
                break;
            }

            if (lastMetaProperty == null)
                break;

            captionParts.add(messages.getTools().getPropertyCaption(lastMetaProperty));
            if (Entity.class.isAssignableFrom(lastMetaProperty.getJavaType())) {
                lastMetaClass = lastMetaProperty.getRange().asClass();
            } else if (Collection.class.isAssignableFrom(lastMetaProperty.getJavaType()) && lastMetaProperty.getRange().isClass()) {
                lastMetaClass = lastMetaProperty.getRange().asClass();
            } else {
                break;
            }
        }

        return Joiner.on(".").join(captionParts);
    }

    @Override
    public MetaProperty getPrimaryKeyPropertyForFts(MetaClass metaClass) {
        return manager.getPrimaryKeyPropertyForFts(metaClass);
    }

    protected void searchByTerm(SearchResult searchResult, String searchTerm, QueryKey queryKey, int maxSearchResults) {
        int currentCount = 0;
        int firstResult = queryKey == null ? 0 : queryKey.getFirstResult();
        boolean emptyIndex = false;
        while (currentCount < maxSearchResults && !emptyIndex) {
            List<EntityInfo> indexResult = luceneSearcher.searchAllField(searchTerm, firstResult, maxSearchResults);
            if (!indexResult.isEmpty()) {
                databaseDataLoader.mergeSearchData(indexResult, false, (callbackResult) -> {
                    EntityInfo entityInfo = callbackResult.getEntityInfo();
                    Entity entity = callbackResult.getEntity();
                    if (callbackResult.isShowInResults()) {
                        SearchResultEntry searchResultEntry = new SearchResultEntry(entityInfo, metadata.getTools().getInstanceName(entity));
                        searchResultEntry.setDirectResult(true);
                        searchResult.addEntry(searchResultEntry);
                    }
                    searchResult.getQueryKey().addEntityInfo(entityInfo);
                });
                currentCount = Math.max(searchResult.getCount(), searchResult.getIdsCount());
                firstResult += maxSearchResults;
                searchResult.getQueryKey().setFirstResult(firstResult);
            } else {
                emptyIndex = true;
            }
        }
    }

    protected void searchByLinks(QueryKey queryKey, SearchResult searchResult, int maxSearchResults) {
        EntityInfo currentEntityKey = queryKey == null ? null : queryKey.getLastId();
        boolean skip = true;
        for (EntityInfo entityInfo : searchResult.getQueryKey().getEntityInfos()) {
            if (searchResult.getCount() >= maxSearchResults) {
                break;
            }
            int firstResult = 0;
            if (currentEntityKey == null) {
                skip = false;
            }
            if (Objects.equals(currentEntityKey, entityInfo) && queryKey != null) {
                skip = false;
                firstResult = queryKey.getLinksFirstResult();
            }
            if (skip) {
                continue;
            }
            currentEntityKey = entityInfo;
            boolean emptyIndex = false;
            while (searchResult.getCount() < maxSearchResults && !emptyIndex) {
                List<EntityInfo> indexResult = luceneSearcher.searchLinksField(entityInfo.toString(), firstResult, maxSearchResults);
                //Previously "links" field of the Lucene document contained a set of linked entities ids.
                //Now a set of {@link EntityInfo} strings is stored there.
                indexResult.addAll(luceneSearcher.searchLinksField(entityInfo.getId(), firstResult, maxSearchResults));
                if (!indexResult.isEmpty()) {
                    databaseDataLoader.mergeSearchData(indexResult, true, (callbackResult) -> {
                        EntityInfo entityInfoWithLink = callbackResult.getEntityInfo();
                        SearchResultEntry entry = searchResult.getEntryByEntityInfo(entityInfoWithLink);
                        if (entry == null) {
                            String instanceName = metadata.getTools().getInstanceName(callbackResult.getEntity());
                            entry = new SearchResultEntry(entityInfoWithLink, instanceName);
                            searchResult.addEntry(entry);
                        }
                        entry.addLinkedEntity(entityInfo);
                    });
                    firstResult += maxSearchResults;
                } else {
                    emptyIndex = true;
                }
            }
            if (!emptyIndex) {
                searchResult.getQueryKey().setLinksFirstResult(firstResult);
                searchResult.getQueryKey().setLastId(currentEntityKey);
                break;
            } else {
                List<EntityInfo> ids = searchResult.getQueryKey().getEntityInfos();
                if (ids.indexOf(currentEntityKey) == ids.size() - 1) {
                    searchResult.getQueryKey().setLastId(null);
                    searchResult.getQueryKey().setLinksFirstResult(0);
                    searchResult.getQueryKey().setSearchByTermAgain(true);
                } else {
                    searchResult.getQueryKey().setLastId(ids.get(ids.indexOf(currentEntityKey) + 1));
                    searchResult.getQueryKey().setLinksFirstResult(0);
                }
            }
        }
    }
}