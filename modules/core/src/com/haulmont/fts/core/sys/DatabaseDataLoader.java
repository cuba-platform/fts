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
import com.haulmont.fts.global.FtsConfig;
import com.haulmont.fts.global.SearchResult;
import com.haulmont.fts.global.SearchResultEntry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        void entryAdded(Object entityId, EntityInfo entityInfo);
    }

    public void mergeSearchData(SearchResult searchResult, List<EntityInfo> mergedData,
                                boolean filterByShowInResults,
                                SearchEntryCallback callback) {
        Map<String, List<EntityInfo>> resultByType = mergedData.stream()
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
            Map<Object, EntityInfo> entityIds = new LinkedHashMap<>();
            for (EntityInfo info : infoList) {
                entityIds.put(info.getId(), info);
            }
            List<Entity> entities = loadEntities(Lists.newArrayList(entityIds.keySet()), metaClass);
            for (Entity entity : entities) {
                MetaProperty idProperty = manager.getPrimaryKeyPropertyForFts(metaClass);
                Object entityId = entity.getValue(idProperty.getName());
                if (entityId instanceof IdProxy) {
                    entityId = ((IdProxy) entityId).getNN();
                }
                EntityInfo entityInfo = entityIds.get(entityId);
                if (showInResults) {
                    searchResult.addEntry(new SearchResultEntry(entityId,
                            entityInfo.getEntityName(),
                            entity.getInstanceName()));
                }
                callback.entryAdded(entityId, entityInfo);
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
