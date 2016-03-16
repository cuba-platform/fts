/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.fts.global.SearchResult;
import junit.framework.TestCase;

public class HitInfoTest extends TestCase {

    public void test() {
        SearchResult.HitInfo hitInfo = new SearchResult.HitInfo();
        hitInfo.init(
                "Контр",
                "^^name Контракт №211 от 21.10.2010 ^^attachment^file Было заключено контрактов на сумму ",
                null
        );

        System.out.println(hitInfo.getHits());
    }
}
