/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.fts.core.jmx;

import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

/**
 * @author krivopustov
 * @version $Id$
 */
public interface FtsManagerMBean {

    boolean isEnabled();
    void setEnabled(boolean value);

    boolean isWriting();

    String processQueue();

    String optimize();

    String upgradeIndexes();

    @ManagedOperationParameters({@ManagedOperationParameter(name = "entityName", description = "")})
    String reindexEntity(String entityName);

    String reindexAll();
}
