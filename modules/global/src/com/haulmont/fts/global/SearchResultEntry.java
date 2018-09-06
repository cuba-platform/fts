/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.global;

import java.io.Serializable;
import java.util.Objects;

public class SearchResultEntry implements Serializable, Comparable<SearchResultEntry> {
    private static final long serialVersionUID = -1033032285547581245L;

    private Object id;
    private String entityName;
    private String caption;

    public SearchResultEntry(Object id, String entityName, String caption) {
        this.id = id;
        this.entityName = entityName;
        this.caption = caption;
    }

    public Object getId() {
        return id;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getCaption() {
        return caption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResultEntry that = (SearchResultEntry) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(entityName, that.entityName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entityName);
    }

    @Override
    public int compareTo(SearchResultEntry o) {
        String c1 = caption == null ? "" : caption;
        String c2 = o.caption == null ? "" : o.caption;
        return c1.compareTo(c2);
    }
}
