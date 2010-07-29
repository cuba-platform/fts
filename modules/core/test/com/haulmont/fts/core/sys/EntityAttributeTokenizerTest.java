/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 28.07.2010 14:33:30
 *
 * $Id$
 */
package com.haulmont.fts.core.sys;

import junit.framework.TestCase;

import java.io.StringReader;

public class EntityAttributeTokenizerTest extends TestCase {

    public void test() {
        EntityAttributeTokenizer tokenizer = new EntityAttributeTokenizer(new StringReader("abcd"));

        boolean b = tokenizer.isTokenChar('^');
        assertTrue(b);
    }
}
