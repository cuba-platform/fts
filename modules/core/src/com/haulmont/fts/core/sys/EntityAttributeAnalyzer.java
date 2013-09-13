/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 26.07.2010 11:27:58
 *
 * $Id$
 */
package com.haulmont.fts.core.sys;

import org.apache.lucene.analysis.Analyzer;

import java.io.Reader;

public class EntityAttributeAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new EntityAttributeTokenizer(reader));
    }
}
