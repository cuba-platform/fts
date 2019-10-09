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

package com.haulmont.fts.core.sys;

import com.google.common.collect.Lists;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.PersistenceSecurity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.IdProxy;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.fts.core.app.FtsManagerAPI;
import com.haulmont.fts.global.EntityInfo;
import com.haulmont.fts.global.FtsConfig;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class is used to filter entities that are found in Lucene index but are not allowed for the user because of row-level security constraints.
 */
@Component(DatabaseDataLoader.NAME)
public class DatabaseDataLoader {

    public static final String NAME = "fts_DatabaseDataLoader";

    @Inject
    protected DataManager dataManager;
    @Inject
    protected FtsManagerAPI manager;
    @Inject
    protected Metadata metadata;
    @Inject
    protected PersistenceSecurity security;
    @Inject
    protected FtsConfig ftsConfig;

    public interface SearchEntryCallback {
        void entryAdded(SearchEntryCallbackResult searchEntryCallbackResult);
    }

    public static class SearchEntryCallbackResult {
        protected Entity entity;
        protected EntityInfo entityInfo;
        protected boolean showInResults;

        public EntityInfo getEntityInfo() {
            return entityInfo;
        }

        public void setEntityInfo(EntityInfo entityInfo) {
            this.entityInfo = entityInfo;
        }

        public boolean isShowInResults() {
            return showInResults;
        }

        public void setShowInResults(boolean searchInResult) {
            this.showInResults = searchInResult;
        }

        public Entity getEntity() {
            return entity;
        }

        public void setEntity(Entity entity) {
            this.entity = entity;
        }
    }

    /**
     * Method accepts a list of {@link EntityInfo} objects that contains entities found by full-text search in Lucene index. Methods checks which of
     * these entities are allowed by row-level security and invokes the {@code callback} for each allowed entity.
     *
     * @param entitiesFoundByFts    a list of entities found by full-text search in Lucene index
     * @param filterByShowInResults indicates that the "show" attribute from the "fts.xml" config should be taken into account. If the {@code
     *                              filterByShowInResults} is true and the entity should not be shown in FTS results then entities of this type will
     *                              be ignored by the method.
     * @param callback              a callback that will be invoked for each entity that is allowed for the user by row-level security
     */
    public void mergeSearchData(List<EntityInfo> entitiesFoundByFts,
                                boolean filterByShowInResults,
                                SearchEntryCallback callback) {
        Map<String, List<EntityInfo>> resultByType = entitiesFoundByFts.stream()
                .collect(Collectors.groupingBy(EntityInfo::getEntityName));
        for (String entityType : resultByType.keySet()) {
            boolean showInResults = manager.showInResults(entityType);
            if (filterByShowInResults && !showInResults) {
                continue;
            }
            MetaClass metaClass = metadata.getSession().getClassNN(entityType);
            if (!security.isEntityOpPermitted(metaClass, EntityOp.READ)) {
                continue;
            }
            List<EntityInfo> infoList = resultByType.get(entityType);
            Map<Object, EntityInfo> entityInfosMap = new LinkedHashMap<>();
            for (EntityInfo info : infoList) {
                entityInfosMap.put(info.getId(), info);
            }
            List<Entity> entities = loadEntities(Lists.newArrayList(entityInfosMap.keySet()), metaClass);
            for (Entity entity : entities) {
                MetaProperty idProperty = manager.getPrimaryKeyPropertyForFts(metaClass);
                Object entityId = entity.getValue(idProperty.getName());
                if (entityId instanceof IdProxy) {
                    entityId = ((IdProxy) entityId).getNN();
                }
                EntityInfo entityInfo = entityInfosMap.get(entityId);
                SearchEntryCallbackResult result = new SearchEntryCallbackResult();
                result.setEntityInfo(entityInfo);
                result.setShowInResults(showInResults);
                result.setEntity(entity);
                callback.entryAdded(result);
            }
        }
    }

    protected List<Entity> loadEntities(List<Object> entityIds, MetaClass metaClass) {
        MetaProperty primaryKeyForFts = manager.getPrimaryKeyPropertyForFts(metaClass);
        List<Entity> result = new ArrayList<>();
        for (List<Object> partition : Lists.partition(entityIds, ftsConfig.getLoadSize())) {
            List<Entity<Object>> partitionResult = dataManager.secure()
                    .load(metaClass.getJavaClass())
                    .view(View.MINIMAL)
                    .query(String.format("select e from %s e where e.%s in :ids", metaClass.getName(), primaryKeyForFts.getName()))
                    .parameter("ids", partition)
                    .list();
            result.addAll(partitionResult);
        }
        return result;
    }
}
