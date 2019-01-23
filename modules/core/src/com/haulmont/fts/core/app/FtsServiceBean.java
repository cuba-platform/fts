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

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.MetadataObject;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.core.sys.*;
import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.global.*;
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

    @Override
    public SearchResult search(String searchTerm, QueryKey queryKey) {
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
            searchResult.getQueryKey().setIds(queryKey.getIds());
            searchResult.getQueryKey().setFirstResult(queryKey.getFirstResult());
        }

        searchByLinks(queryKey, searchResult, maxSearchResults);

        if (!searchByTerm && searchResult.getCount() == 0 && searchResult.getQueryKey().isSearchByTermAgain()) {
            return search(searchTerm, searchResult.getQueryKey());
        } else {
            return searchResult;
        }
    }

    @Override
    public SearchResult search(String searchTerm, List<String> entityNames) {
        //first search among entities with names from entityNames method parameter
        List<EntityInfo> allFieldResults = luceneSearcher.searchAllField(searchTerm, entityNames);

        SearchResult searchResult = new SearchResult(searchTerm);
        for (EntityInfo entityInfo : allFieldResults) {
            searchResult.addHit(entityInfo.getId(), entityInfo.getEntityName(), entityInfo.getText(), createNormalizer());
            //we don't reload entity because we don't need entity caption
            SearchResultEntry entry = new SearchResultEntry(entityInfo.getId(), entityInfo.getEntityName(),
                    entityInfo.getId().toString());
            searchResult.addEntry(entry);
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
                searchResult.addLinkedHit(entityWithLinkInfo.getId(), entityWithLinkInfo.getEntityName(), linkedEntitiesInfo.getText(),
                        linkedEntitiesInfo.getEntityName(), createNormalizer());
                searchResult.addEntry(new SearchResultEntry(entityWithLinkInfo.getId(), entityWithLinkInfo.getEntityName(),
                        entityWithLinkInfo.getId().toString()));
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

    protected void searchByTerm(SearchResult searchResult, String searchTerm, QueryKey queryKey, int maxSearchResults) {
        int currentCount = 0;
        int firstResult = queryKey == null ? 0 : queryKey.getFirstResult();
        boolean emptyIndex = false;
        while (currentCount < maxSearchResults && !emptyIndex) {
            List<EntityInfo> indexResult = luceneSearcher.searchAllField(searchTerm, firstResult, maxSearchResults);
            if (!indexResult.isEmpty()) {
                databaseDataLoader.mergeSearchData(searchResult, indexResult, false, (entityId, entityInfo) -> {
                    searchResult.addHit(entityId, entityInfo.getEntityName(), entityInfo.getText(),
                            createNormalizer());
                    searchResult.getQueryKey().addId(entityId, entityInfo.getEntityName(), entityInfo.getText());
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
        EntityKey currentEntityKey = queryKey == null ? null : queryKey.getLastId();
        boolean skip = true;
        for (EntityKey entityKey : searchResult.getQueryKey().getIds()) {
            if (searchResult.getCount() >= maxSearchResults) {
                break;
            }
            int firstResult = 0;
            if (currentEntityKey == null) {
                skip = false;
            }
            if (Objects.equals(currentEntityKey, entityKey) && queryKey != null) {
                skip = false;
                firstResult = queryKey.getLinksFirstResult();
            }
            if (skip) {
                continue;
            }
            currentEntityKey = entityKey;
            boolean emptyIndex = false;
            while (searchResult.getCount() < maxSearchResults && !emptyIndex) {
                List<EntityInfo> indexResult = luceneSearcher.searchLinksField(entityKey.toString(), firstResult, maxSearchResults);
                //Previously "links" field of the Lucene document contained a set of linked entities ids.
                //Now a set of {@link EntityInfo} strings is stored there.
                indexResult.addAll(luceneSearcher.searchLinksField(entityKey.getId(), firstResult, maxSearchResults));
                if (!indexResult.isEmpty()) {
                    databaseDataLoader.mergeSearchData(searchResult, indexResult, true, (entityIdWithLink, entityInfoWithLink) -> {
                        searchResult.addLinkedHit(entityIdWithLink, entityInfoWithLink.getEntityName(), entityKey.getText(),
                                entityKey.getEntityName(), createNormalizer());
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
                List<EntityKey> ids = searchResult.getQueryKey().getIds();
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


    protected Normalizer createNormalizer() {
        return new MorphologyNormalizer();
    }
}