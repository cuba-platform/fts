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

public class IndexingException extends Exception {
    private static final long serialVersionUID = -406668009906992897L;

    private String entityName;
    private Object entityId;
    private EntityType entityType;

    public IndexingException(EntityType entityType, Throwable cause) {
        this(null, null, null, entityType, cause);
    }

    public IndexingException(String entityName, Object entityId, EntityType entityType, Throwable cause) {
        this(entityName, entityId, null, entityType, cause);
    }

    public IndexingException(String entityName, Object entityId, String message, EntityType entityType, Throwable cause) {
        super(message, cause);
        this.entityName = entityName;
        this.entityId = entityId;
        this.entityType = entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getEntityId() {
        return entityId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public enum EntityType {
        FILE,
        OTHER
    }
}
