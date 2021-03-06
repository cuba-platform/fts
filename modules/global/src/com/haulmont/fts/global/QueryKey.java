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
import java.util.Collections;
import java.util.List;

/**
 * Represents index of the current page:
 *  - shifts from the beginning of searches by term and by links
 *  - entity ids - that searched by terms
 *  - last entity id for which system finds linked entities
 * Query key is used for loading data for the next page
 */
public class QueryKey implements Serializable {
    private static final long serialVersionUID = -8475515682993753239L;

    protected int firstResult;
    protected int linksFirstResult;
    protected EntityInfo lastId;
    protected List<EntityInfo> entityInfos = new ArrayList<>();
    protected boolean searchByTermAgain;

    public int getFirstResult() {
        return firstResult;
    }

    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    public int getLinksFirstResult() {
        return linksFirstResult;
    }

    public void setLinksFirstResult(int linksFirstResult) {
        this.linksFirstResult = linksFirstResult;
    }

    public EntityInfo getLastId() {
        return lastId;
    }

    public void setLastId(EntityInfo lastId) {
        this.lastId = lastId;
    }

    public List<EntityInfo> getEntityInfos() {
        return Collections.unmodifiableList(entityInfos);
    }

    public void addEntityInfo(EntityInfo entityInfo) {
        entityInfos.add(entityInfo);
    }

    public void setEntityInfos(List<EntityInfo> entityInfos) {
        this.entityInfos = new ArrayList<>(entityInfos);
    }

    public boolean isSearchByTermAgain() {
        return searchByTermAgain;
    }

    public void setSearchByTermAgain(boolean searchByTermAgain) {
        this.searchByTermAgain = searchByTermAgain;
    }
}
