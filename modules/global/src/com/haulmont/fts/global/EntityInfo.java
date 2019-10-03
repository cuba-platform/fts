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

/**
 * Class holds an information about the entity
 */
public class EntityInfo implements Serializable {

    private String entityName;
    private Object id;

    public EntityInfo(String entityName, Object id) {
        this.id = id;
        this.entityName = entityName;
    }

    public Object getId() {
        return id;
    }

    public String getEntityName() {
        return entityName;
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
}