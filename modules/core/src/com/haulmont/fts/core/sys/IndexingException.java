/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
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
