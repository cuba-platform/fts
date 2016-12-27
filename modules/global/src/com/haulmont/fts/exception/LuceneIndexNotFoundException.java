/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.exception;

/**
 */
public class LuceneIndexNotFoundException extends RuntimeException {
    public LuceneIndexNotFoundException(Throwable cause) {
        super(cause);
    }

    public LuceneIndexNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public LuceneIndexNotFoundException() {
    }

    public LuceneIndexNotFoundException(String message) {
        super(message);
    }

    public LuceneIndexNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
