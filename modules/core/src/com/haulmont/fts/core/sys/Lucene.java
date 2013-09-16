/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.core.sys;

import org.apache.lucene.store.Directory;

public class Lucene {

    protected static final String FLD_ENTITY = "entity";
    protected static final String FLD_ID = "id";
    protected static final String FLD_ALL = "all";
    protected static final String FLD_LINKS = "links";
    protected static final String FLD_MORPHOLOGY_ALL = "morphologyAll";

    protected Directory directory;

    public Lucene(Directory directory) {
        this.directory = directory;
    }

}
