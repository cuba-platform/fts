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
