/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.07.2010 18:28:39
 *
 * $Id$
 */
package com.haulmont.fts.core.sys;

import org.apache.lucene.store.Directory;

public class Lucene {

    protected static final String FLD_ENTITY = "entity";
    protected static final String FLD_ID = "id";
    protected static final String FLD_ALL = "all";
    protected static final String FLD_LINKS = "links";

    protected Directory directory;

    public Lucene(Directory directory) {
        this.directory = directory;
    }

}
