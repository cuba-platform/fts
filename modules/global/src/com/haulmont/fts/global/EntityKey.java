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
