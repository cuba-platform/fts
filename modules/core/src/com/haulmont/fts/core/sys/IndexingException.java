/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.fts.core.sys;

import java.util.UUID;

/**
 * @author gorelov
 * @version $Id$
 */
public class IndexingException extends Exception {
    private static final long serialVersionUID = -406668009906992897L;

    private String entityName;
    private UUID entityId;
    private EntityType entityType;

    public IndexingException(EntityType entityType, Throwable cause) {
        this(null, null, null, entityType, cause);
    }

    public IndexingException(String entityName, UUID entityId, EntityType entityType, Throwable cause) {
        this(entityName, entityId, null, entityType, cause);
    }

    public IndexingException(String entityName, UUID entityId, String message, EntityType entityType, Throwable cause) {
        super(message, cause);
        this.entityName = entityName;
        this.entityId = entityId;
        this.entityType = entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    public UUID getEntityId() {
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
