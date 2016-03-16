/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.sys;

import junit.framework.TestCase;

import java.io.StringReader;

public class EntityAttributeTokenizerTest extends TestCase {

    public void test() {
        EntityAttributeTokenizer tokenizer = new EntityAttributeTokenizer();

        boolean b = tokenizer.isTokenChar('^');
        assertTrue(b);

        b = tokenizer.isTokenChar('"');
        assertFalse(b);

        b = tokenizer.isTokenChar('$');
        assertTrue(b);
    }
}
