/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 28.07.2010 15:03:34
 *
 * $Id$
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
