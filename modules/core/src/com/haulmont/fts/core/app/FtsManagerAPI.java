/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.fts.core.app;

import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.fts.core.sys.EntityDescr;
import org.apache.lucene.store.Directory;

import java.util.List;
import java.util.Map;

/**
 * @author krivopustov
 * @version $Id$
 */
public interface FtsManagerAPI {

    String NAME = "cuba_FtsManager";

    Directory getDirectory();

    List<BaseEntity> getSearchableEntities(BaseEntity entity);

    public Map<String, EntityDescr> getDescrByName();

    int processQueue();

    String optimize();

    String upgradeIndexes();

    boolean showInResults(String entityName);

    boolean isEnabled();

    void setEnabled(boolean value);

    boolean isWriting();

    void deleteIndexForEntity(String entityName);

    void deleteIndex();

    int reindexEntity(String entityName);

    int reindexAll();
}
