/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.app;

public class TestFtsManager extends FtsManager {
    @Override
    protected boolean isApplicationContextStarted() {
        return true;
    }
}
