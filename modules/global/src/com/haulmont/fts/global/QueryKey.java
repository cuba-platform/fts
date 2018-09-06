/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
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
    protected EntityKey lastId;
    protected List<EntityKey> ids = new ArrayList<>();
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

    public EntityKey getLastId() {
        return lastId;
    }

    public void setLastId(EntityKey lastId) {
        this.lastId = lastId;
    }

    public List<EntityKey> getIds() {
        return Collections.unmodifiableList(ids);
    }

    public void addId(Object id, String entityName, String text) {
        ids.add(new EntityKey(id, entityName, text));
    }

    public void setIds(List<EntityKey> ids) {
        this.ids = new ArrayList<>(ids);
    }

    public boolean isSearchByTermAgain() {
        return searchByTermAgain;
    }

    public void setSearchByTermAgain(boolean searchByTermAgain) {
        this.searchByTermAgain = searchByTermAgain;
    }
}
