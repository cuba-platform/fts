/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.fts.global.FTS;
import junit.framework.TestCase;

public class TokenizerTest extends TestCase {

    public void test() {
        FTS.Tokenizer tokenizer = new FTS.Tokenizer("f");
        while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
            System.out.println(s + " (" + tokenizer.getTokenStart() + "," + tokenizer.getTokenEnd() + ")");
        }

        tokenizer = new FTS.Tokenizer("Было заключено контрактов на сумму ");
        while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
            System.out.println(s + " (" + tokenizer.getTokenStart() + "," + tokenizer.getTokenEnd() + ")");
        }

        tokenizer = new FTS.Tokenizer("[TM-0008] Было заключено контрактов на сумму 765271");
        while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
            System.out.println(s + " (" + tokenizer.getTokenStart() + "," + tokenizer.getTokenEnd() + ")");
        }
    }
}
