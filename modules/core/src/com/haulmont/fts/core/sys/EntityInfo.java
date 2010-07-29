/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.07.2010 18:54:17
 *
 * $Id$
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
