/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.web.ui.results;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManager.OpenType;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.web.gui.components.mainwindow.FtsSearchLauncher;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.global.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

@Component(FtsSearchLauncher.NAME)
public class FtsSearchLauncherImpl implements FtsSearchLauncher {

    @Inject
    protected Messages messages;
    @Inject
    protected FtsService ftsService;
    @Inject
    protected WindowConfig windowConfig;

    @Override
    public void search(FrameOwner origin, String searchTerm) {
        checkNotNullArgument(origin);

        if (StringUtils.isBlank(searchTerm)) {
            Notifications notifications = origin.getScreenContext().getNotifications();

            notifications.create()
                    .setCaption(messages.getMessage(FtsSearchLauncherImpl.class, "noSearchTerm"))
                    .setType(Notifications.NotificationType.HUMANIZED)
                    .show();
        } else {
            String searchTermPreprocessed = searchTerm.trim().toLowerCase();

            SearchResult searchResult = ftsService.search(searchTermPreprocessed);

            if (searchResult.isEmpty()) {
                Notifications notifications = origin.getScreenContext().getNotifications();

                notifications.create()
                        .setCaption(messages.getMessage(FtsSearchLauncherImpl.class, "notFound"))
                        .setType(Notifications.NotificationType.HUMANIZED)
                        .show();
            } else {
                WindowInfo windowInfo = windowConfig.getWindowInfo("ftsSearchResults");

                WindowManager wm = (WindowManager) origin.getScreenContext().getScreens();
                wm.openWindow(windowInfo,
                        OpenType.NEW_TAB,
                        ParamsMap.of(
                                "searchTerm", searchTerm,
                                "searchResult", searchResult)
                );
            }
        }
    }
}