/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 25.06.2010 15:23:26
 *
 * $Id$
 */
package com.haulmont.fts.core.app;

import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

public interface FtsManagerMBean {

    boolean isEnabled();
    void setEnabled(boolean value);

    boolean isWriting();

    String jmxProcessQueue();

    String jmxOptimize();

    @ManagedOperationParameters({@ManagedOperationParameter(name = "entityName", description = "")})
    String jmxReindexEntity(String entityName);

    String jmxReindexAll();
}
