/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.core.sys;

import org.apache.lucene.analysis.Analyzer;

import java.io.Reader;

public class EntityAttributeAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return new TokenStreamComponents(new EntityAttributeTokenizer());
    }
}
