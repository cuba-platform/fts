/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 24.06.2010 18:06:59
 *
 * $Id$
 */
package com.haulmont.fts.core.app;

import com.haulmont.cuba.core.entity.BaseEntity;

public interface FtsManagerAPI {

    String NAME = "cuba_FtsManager";

    boolean isSearchable(BaseEntity entity);
}
