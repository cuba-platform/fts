/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.web.ui.results;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.Frame;
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

    protected Map<String, Object> params;

    public SearchLauncher(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public Window call() throws Exception {
        if (params == null)
            throw new IllegalStateException("params is null");

        Messages messages = AppBeans.get(Messages.NAME);
        String searchTerm = (String) params.get("searchTerm");
        if (StringUtils.isBlank(searchTerm)) {
            App.getInstance().getWindowManager().showNotification(
                    messages.getMessage(getClass(), "noSearchTerm"), Frame.NotificationType.HUMANIZED);
            return null;
        } else {
            searchTerm = searchTerm.trim();
            FtsService service = AppBeans.get(FtsService.NAME);
            SearchResult searchResult = service.search(searchTerm.toLowerCase());

            WindowManager windowManager = App.getInstance().getWindowManager();

            if (searchResult.isEmpty()) {
                windowManager.showNotification(
                        messages.getMessage(getClass(), "notFound"), Frame.NotificationType.HUMANIZED);
                return null;
            } else {
                params.put("searchResult", searchResult);
                WindowInfo windowInfo = AppBeans.get(WindowConfig.class).getWindowInfo("ftsSearchResults");

                return windowManager.openWindow(windowInfo, WindowManager.OpenType.NEW_TAB, params);
            }
        }
    }
}