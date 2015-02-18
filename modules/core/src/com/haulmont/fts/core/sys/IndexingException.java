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

    public IndexingException(Throwable cause) {
        this(null, null, null, cause);
    }

    public IndexingException(String entityName, UUID entityId, Throwable cause) {
        this(entityName, entityId, null, cause);
    }

    public IndexingException(String entityName, UUID entityId, String message, Throwable cause) {
        super(message, cause);
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public IndexingException(String entityName, UUID entityId, String message) {
        this(entityName, entityId, message, null);
    }

    public IndexingException(String entityName, UUID entityId) {
        this(entityName, entityId, null, null);
    }

    public String getEntityName() {
        return entityName;
    }

    public UUID getEntityId() {
        return entityId;
    }
}
