/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.07.2010 14:44:03
 *
 * $Id$
 */
package com.haulmont.fts.web.ui.results;

import com.haulmont.bali.datastruct.Pair;
import com.haulmont.chile.core.model.Session;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.MessageUtils;
import com.haulmont.cuba.core.global.MetadataProvider;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.NoSuchScreenException;
import com.haulmont.cuba.gui.ServiceLocator;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppWindow;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.global.SearchResult;
import com.vaadin.data.Property;
import com.vaadin.event.FieldEvents;
import com.vaadin.terminal.Paintable;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.BaseTheme;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class SearchResultsWindow extends AbstractWindow {

    protected AbstractOrderedLayout contentLayout;

    protected FtsService service;

    protected SearchResult searchResult;

    public SearchResultsWindow(IFrame frame) {
        super(frame);
    }

    @Override
    protected void init(Map<String, Object> params) {
        contentLayout = (AbstractOrderedLayout) WebComponentsHelper.unwrap(getComponent("contentBox"));

        String searchTerm = (String) params.get("searchTerm");
        if (StringUtils.isBlank(searchTerm)) {
            setCaption(getMessage("caption"));
            Label label = new Label(getMessage("noSearchTerm"));
            label.setStyleName("h2");
            contentLayout.addComponent(label);

        } else {
            searchTerm = searchTerm.trim();
            setCaption(getMessage("caption") + ": " + searchTerm);

            service = ServiceLocator.lookup(FtsService.NAME);
            searchResult = service.search(searchTerm.toLowerCase());

            if (searchResult.isEmpty()) {
                Label label = new Label(getMessage("notFound"));
                label.setStyleName("h2");
                contentLayout.addComponent(label);

            } else {
                Session metadata = MetadataProvider.getSession();

                List<Pair<String, String>> entities = new ArrayList<Pair<String, String>>();
                for (String entityName : searchResult.getEntities()) {
                    entities.add(new Pair<String, String>(
                            entityName,
                            MessageUtils.getEntityCaption(metadata.getClass(entityName))
                    ));
                }
                Collections.sort(
                        entities,
                        new Comparator<Pair<String, String>>() {
                            public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                                return o1.getSecond().compareTo(o2.getSecond());
                            }
                        }
                );

                for (Pair<String, String> entityPair : entities) {
                    Label separator = new Label("<hr/>");
                    separator.setContentMode(Label.CONTENT_XHTML);

                    contentLayout.addComponent(separator);

                    GridLayout grid = new GridLayout(2, 1);

                    Label entityLabel = new Label(entityPair.getSecond());
                    entityLabel.setStyleName("h2");
                    entityLabel.setWidth(200, Label.UNITS_PIXELS);
                    grid.addComponent(entityLabel, 0, 0);

                    VerticalLayout instancesLayout = new VerticalLayout();
                    displayInstances(entityPair.getFirst(), instancesLayout);
                    grid.addComponent(instancesLayout, 1, 0);

                    contentLayout.addComponent(grid);
                }
            }
        }
    }

    private void displayInstances(String entityName, VerticalLayout instancesLayout) {
        List<SearchResult.Entry> entries = searchResult.getEntries(entityName);
        for (SearchResult.Entry entry : entries) {
            HorizontalLayout instanceLayout = new HorizontalLayout();

            Button instanceBtn = new Button(entry.getCaption());
            instanceBtn.setStyleName(BaseTheme.BUTTON_LINK);
            instanceBtn.addListener(new InstanceClickListener(entityName, entry.getId()));

            instanceLayout.addComponent(instanceBtn);
            instanceLayout.setComponentAlignment(instanceBtn, com.vaadin.ui.Alignment.MIDDLE_LEFT);

            instancesLayout.addComponent(instanceLayout);
        }
        if (!searchResult.getIds(entityName).isEmpty()) {
            HorizontalLayout instanceLayout = new HorizontalLayout();

            Button instanceBtn = new Button(getMessage("more"));
            instanceBtn.setStyleName(BaseTheme.BUTTON_LINK);
            instanceBtn.addListener(new MoreClickListener(entityName, instancesLayout));

            instanceLayout.addComponent(instanceBtn);
            instanceLayout.setComponentAlignment(instanceBtn, com.vaadin.ui.Alignment.MIDDLE_LEFT);

            instancesLayout.addComponent(instanceLayout);
        }
    }

    private class InstanceClickListener implements Button.ClickListener {

        private String entityName;
        private UUID entityId;

        public InstanceClickListener(String entityName, UUID entityId) {
            this.entityName = entityName;
            this.entityId = entityId;
        }

        public void buttonClick(Button.ClickEvent event) {
            String windowId = entityName + ".edit";

            LoadContext lc = new LoadContext(MetadataProvider.getSession().getClass(entityName));
            lc.setView(View.MINIMAL);
            lc.setId(entityId);
            Entity entity = getDsContext().getDataService().load(lc);

            WindowManager.OpenType openType = AppWindow.Mode.TABBED.equals(App.getInstance().getAppWindow().getMode()) ?
                    WindowManager.OpenType.NEW_TAB : WindowManager.OpenType.THIS_TAB;
            openEditor(windowId, entity, openType);
        }
    }

    private class MoreClickListener implements Button.ClickListener {

        private String entityName;
        private VerticalLayout instancesLayout;

        public MoreClickListener(String entityName, VerticalLayout instancesLayout) {
            this.entityName = entityName;
            this.instancesLayout = instancesLayout;
        }

        public void buttonClick(Button.ClickEvent event) {
            searchResult = service.expandResult(searchResult, entityName);

            instancesLayout.removeAllComponents();
            displayInstances(entityName, instancesLayout);
        }
    }
}
