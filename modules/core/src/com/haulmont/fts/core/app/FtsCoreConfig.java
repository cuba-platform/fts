/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.app;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.type.Factory;
import com.haulmont.cuba.core.config.type.StringListTypeFactory;

import java.util.List;

@Source(type = SourceType.DATABASE)
public interface FtsCoreConfig extends Config {

    @Property("fts.indexingBatchSize")
    @DefaultInt(300)
    int getIndexingBatchSize();

    @Property("fts.indexDir")
    String getIndexDir();

    @Property("fts.maxSearchResults")
    @DefaultInt(100)
    int getMaxSearchResults();

    @Property("fts.searchResultsBatchSize")
    @DefaultInt(5)
    int getSearchResultsBatchSize();

    @Property("fts.storeContentInIndex")
    @DefaultBoolean(true)
    boolean getStoreContentInIndex();

    @Property("fts.indexingHosts")
    @Factory(factory = StringListTypeFactory.class)
    List<String> getIndexingHosts();

    @Property("fts.reindexBatchSize")
    @DefaultInt(5000)
    int getReindexBatchSize();
}
