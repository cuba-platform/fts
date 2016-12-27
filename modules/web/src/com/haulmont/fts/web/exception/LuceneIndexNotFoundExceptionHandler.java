/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.web.exception;

import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.exception.AbstractGenericExceptionHandler;
import com.haulmont.fts.exception.LuceneIndexNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 */
@Component("fts_IndexNotFoundExceptionHandler")
public class LuceneIndexNotFoundExceptionHandler extends AbstractGenericExceptionHandler {

    @Inject
    protected Messages messages;

    public LuceneIndexNotFoundExceptionHandler() {
        super(LuceneIndexNotFoundException.class.getName());
    }

    @Override
    protected void doHandle(String className, String message, @Nullable Throwable throwable, WindowManager windowManager) {
        String msg = messages.getMessage(LuceneIndexNotFoundExceptionHandler.class, "indexNotFound.msg");
        windowManager.showNotification(msg, Frame.NotificationType.ERROR);
    }
}
