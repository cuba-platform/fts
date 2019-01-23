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

import java.util.Objects;

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
        return Objects.equals(entityName, that.entityName) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityName, id);
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