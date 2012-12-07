/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.fts.core.app;

import com.haulmont.cuba.core.entity.BaseEntity;
import org.apache.lucene.store.Directory;

import java.util.List;

/**
 * @author krivopustov
 * @version $Id$
 */
public interface FtsManagerAPI {

    String NAME = "cuba_FtsManager";

    Directory getDirectory();

    List<BaseEntity> getSearchableEntities(BaseEntity entity);

    int processQueue();

    String optimize();

    boolean showInResults(String entityName);

    boolean isEnabled();

    void setEnabled(boolean value);

    boolean isWriting();

    void deleteIndexForEntity(String entityName);

    void deleteIndex();

    int reindexEntity(String entityName);

    int reindexAll();
}
