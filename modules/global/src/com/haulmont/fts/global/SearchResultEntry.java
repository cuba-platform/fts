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

package com.haulmont.fts.global;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class represents a single entry of the {@link SearchResult}. Each entry is linked with a single entity.
 * <p>
 * The {@link #isDirectResult} field is true if the searchTerm was found among local properties of the entity
 * <p>
 * The {@link #linkedEntityInfos} collection contains a list of linked entities which contain the searchTerm
 * <p>
 * The {@link #hitInfos} collection contains an information in which field the searchTerm was found (both for local fields and for linked entities)
 * and also the hitInfo contains a piece highlighted text which includes the search term
 */
public class SearchResultEntry implements Serializable {
    private static final long serialVersionUID = -1033032285547581245L;

    protected EntityInfo entityInfo;
    protected String instanceName;
    protected boolean isDirectResult;
    protected List<EntityInfo> linkedEntityInfos = new ArrayList<>();
    protected List<HitInfo> hitInfos = new ArrayList<>();

    public SearchResultEntry(EntityInfo entityInfo) {
        this.entityInfo = entityInfo;
    }

    public SearchResultEntry(EntityInfo entityInfo, String instanceName) {
        this.entityInfo = entityInfo;
        this.instanceName = instanceName;
    }

    public EntityInfo getEntityInfo() {
        return entityInfo;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public List<EntityInfo> getLinkedEntityInfos() {
        return linkedEntityInfos;
    }

    public void addLinkedEntity(EntityInfo linkedEntityInfo) {
        linkedEntityInfos.add(linkedEntityInfo);
    }

    public List<HitInfo> getHitInfos() {
        return hitInfos;
    }

    public void setHitInfos(List<HitInfo> hitInfos) {
        this.hitInfos = hitInfos;
    }

    /**
     * @return true if the searchTerm was found in own fields of the entity (in the "all" field of the Lucene document)
     */
    public boolean isDirectResult() {
        return isDirectResult;
    }

    public void setDirectResult(boolean directResult) {
        isDirectResult = directResult;
    }
}
