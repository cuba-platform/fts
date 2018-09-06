/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts;

import com.haulmont.cuba.testsupport.TestContainer;

import java.util.ArrayList;
import java.util.Arrays;

public class FtsTestContainer extends TestContainer {

    public FtsTestContainer() {
        super();
        appComponents = new ArrayList<>(Arrays.asList(
                "com.haulmont.cuba",
                "com.haulmont.fts"
        ));
        appPropertiesFiles = Arrays.asList("cuba-app.properties", "fts-test-app.properties");
        initDbProperties();
    }

    private void initDbProperties() {
        dbDriver = "org.hsqldb.jdbc.JDBCDriver";
        dbUrl = "jdbc:hsqldb:hsql://localhost:9112/ftsdb";
        dbUser = "sa";
        dbPassword = "";
    }

    public static class Common extends FtsTestContainer {

        public static final FtsTestContainer.Common INSTANCE = new FtsTestContainer.Common();

        private static volatile boolean initialized;

        private Common() {
        }

        @Override
        public void before() throws Throwable {
            if (!initialized) {
                super.before();
                initialized = true;
            }
            setupContext();
        }

        @Override
        public void after() {
            cleanupContext();
            // never stops - do not call super
        }
    }
}