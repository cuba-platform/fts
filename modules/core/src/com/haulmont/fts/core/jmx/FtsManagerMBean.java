/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
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

    String upgrade();

    @ManagedOperationParameters({@ManagedOperationParameter(name = "entityName", description = "")})
    String reindexEntity(String entityName);

    String reindexAll();
}
