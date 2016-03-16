/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.sys;

import java.util.UUID;

public class EntityInfo implements Comparable<EntityInfo> {

    private String name;
    private UUID id;
    private String text;
    private boolean inLinks;

    public EntityInfo(String name, UUID id, String text, boolean inLinks) {
        this.id = id;
        this.name = name;
        this.text = text;
        this.inLinks = inLinks;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
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
        return name + "-" + id;
    }

    public int compareTo(EntityInfo o) {
        if (name == null || o.name == null)
            return 0;
        else
            return name.compareTo(o.name);
    }
}
