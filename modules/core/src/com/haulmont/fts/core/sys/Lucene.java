/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.sys;

import org.apache.lucene.store.Directory;

public class Lucene {

    public static final String FLD_ENTITY = "entity";
    public static final String FLD_ID = "id";
    public static final String FLD_ALL = "all";
    public static final String FLD_LINKS = "links";
    public static final String FLD_MORPHOLOGY_ALL = "morphologyAll";

    protected Directory directory;

    public Lucene(Directory directory) {
        this.directory = directory;
    }

}
