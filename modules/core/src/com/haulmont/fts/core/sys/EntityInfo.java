/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.sys;

public class EntityInfo implements Comparable<EntityInfo> {

    private String entityName;
    private Object id;
    private String text;
    private boolean inLinks;

    public EntityInfo(String name, Object id, String text, boolean inLinks) {
        this.id = id;
        this.entityName = name;
        this.text = text;
        this.inLinks = inLinks;
    }

    public Object getId() {
        return id;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getText() {
        return text;
    }

    public boolean isInLinks() {
        return inLinks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityInfo that = (EntityInfo) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return entityName + "-" + id;
    }

    @Override
    public int compareTo(EntityInfo o) {
        if (entityName == null || o.entityName == null)
            return 0;
        else
            return entityName.compareTo(o.entityName);
    }
}