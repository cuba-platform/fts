/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 26.07.2010 12:12:13
 *
 * $Id$
 */
package com.haulmont.fts.web.ui.results;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.web.App;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.global.SearchResult;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.concurrent.Callable;

public class SearchLauncher implements Callable<Window> {

    private Map<String, Object> params;

    public SearchLauncher(Map<String, Object> params) {
        this.params = params;
    }

    public Window call() throws Exception {
        if (params == null)
            throw new IllegalStateException("params is null");

        Messages messages = AppBeans.get(Messages.NAME);
        String searchTerm = (String) params.get("searchTerm");
        if (StringUtils.isBlank(searchTerm)) {
            App.getInstance().getWindowManager().showNotification(
                    messages.getMessage(getClass(), "noSearchTerm"), IFrame.NotificationType.HUMANIZED);
            return null;
        } else {
            searchTerm = searchTerm.trim();
            FtsService service = AppBeans.get(FtsService.NAME);
            SearchResult searchResult = service.search(searchTerm.toLowerCase());

            WindowManager windowManager = App.getInstance().getWindowManager();

            if (searchResult.isEmpty()) {
                windowManager.showNotification(
                        messages.getMessage(getClass(), "notFound"), IFrame.NotificationType.HUMANIZED);
                return null;
            } else {
                params.put("searchResult", searchResult);
                WindowInfo windowInfo = AppBeans.get(WindowConfig.class).getWindowInfo("fts$SearchResults");

                return windowManager.openWindow(windowInfo, WindowManager.OpenType.NEW_TAB, params);
            }
        }
    }
}
