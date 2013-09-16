/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.fts.global.FTS;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;

import java.io.Reader;

public class EntityAttributeTokenizer extends CharTokenizer {

    public EntityAttributeTokenizer(AttributeSource.AttributeFactory factory, Reader input) {
        super(Version.LUCENE_44, factory, input);
    }

    public EntityAttributeTokenizer(Reader input) {
        super(Version.LUCENE_44, input);
    }

    protected int normalize(int c) {
        return Character.toLowerCase(c);
    }

    @Override
    protected boolean isTokenChar(int c) {
        return FTS.isTokenChar(c);
    }
}
