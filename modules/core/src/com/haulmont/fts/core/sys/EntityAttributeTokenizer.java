/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 26.07.2010 11:18:44
 *
 * $Id$
 */
package com.haulmont.fts.core.sys;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.AttributeSource;

import java.io.Reader;

public class EntityAttributeTokenizer extends CharTokenizer {

    public EntityAttributeTokenizer(AttributeFactory factory, Reader input) {
        super(factory, input);
    }

    public EntityAttributeTokenizer(Reader input) {
        super(input);
    }

    public EntityAttributeTokenizer(AttributeSource source, Reader input) {
        super(source, input);
    }

    @Override
    protected boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '/' || c == '\\' || c == '^';
    }

    @Override
    protected char normalize(char c) {
        return Character.toLowerCase(c);
    }
}
