/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.global;

import java.io.Serializable;
import java.util.Objects;

public class EntityKey implements Serializable {
    private static final long serialVersionUID = -9098046057448670825L;

    protected Object id;
    protected String entityName;
    protected String text;

    public EntityKey(Object id, String entityName, String text) {
        this.id = id;
        this.entityName = entityName;
        this.text = text;
    }

    public EntityKey(Object id, String entityName) {
        this(id, entityName, null);
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityKey entityKey = (EntityKey) o;
        return Objects.equals(entityName, entityKey.entityName) &&
                Objects.equals(id, entityKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityName, id);
    }

    @Override
    public String toString() {
        return entityName + "-" + id;
    }
}
