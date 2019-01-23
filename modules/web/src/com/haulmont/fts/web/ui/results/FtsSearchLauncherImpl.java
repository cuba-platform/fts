/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.haulmont.fts.web.ui.results;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Notifications.NotificationType;
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
import static com.haulmont.cuba.gui.screen.UiControllerUtils.getScreenContext;

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
            Notifications notifications = getScreenContext(origin).getNotifications();

            notifications.create(NotificationType.HUMANIZED)
                    .withCaption(messages.getMessage(FtsSearchLauncherImpl.class, "noSearchTerm"))
                    .show();
        } else {
            String searchTermPreprocessed = searchTerm.trim().toLowerCase();

            SearchResult searchResult = ftsService.search(searchTermPreprocessed);

            if (searchResult.isEmpty()) {
                Notifications notifications = getScreenContext(origin).getNotifications();

                notifications.create(NotificationType.HUMANIZED)
                        .withCaption(messages.getMessage(FtsSearchLauncherImpl.class, "notFound"))
                        .show();
            } else {
                WindowInfo windowInfo = windowConfig.getWindowInfo("ftsSearchResults");

                WindowManager wm = (WindowManager) getScreenContext(origin).getScreens();
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