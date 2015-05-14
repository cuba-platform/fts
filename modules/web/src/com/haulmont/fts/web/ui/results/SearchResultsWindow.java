/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.web.ui.results;

import com.haulmont.bali.datastruct.Pair;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.Session;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.mainwindow.AppWorkArea;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppWindow;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.toolkit.ui.CubaButton;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.global.SearchResult;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.BaseTheme;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
public class SearchResultsWindow extends AbstractWindow {

    protected AbstractOrderedLayout contentLayout;

    @Inject
    protected FtsService service;

    @Inject
    protected Metadata metadata;

    protected SearchResult searchResult;

    protected MetaClass fileMetaClass;

    @Override
    public void init(Map<String, Object> params) {
        fileMetaClass = metadata.getSession().getClassNN(FileDescriptor.class);

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

            searchResult = (SearchResult) params.get("searchResult");
            if (searchResult == null)
                searchResult = service.search(searchTerm.toLowerCase());

            paintResult(searchResult);
        }
    }

    protected void paintResult(SearchResult searchResult) {
        if (searchResult.isEmpty()) {
            Label label = new Label(getMessage("notFound"));
            label.setStyleName("h2");
            contentLayout.addComponent(label);
        } else {
            Session session = metadata.getSession();

            List<Pair<String, String>> entities = new ArrayList<>();
            for (String entityName : searchResult.getEntities()) {
                entities.add(new Pair<>(
                        entityName,
                        messages.getTools().getEntityCaption(session.getClass(entityName))
                ));
            }
            Collections.sort(
                    entities,
                    new Comparator<Pair<String, String>>() {
                        @Override
                        public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                            return o1.getSecond().compareTo(o2.getSecond());
                        }
                    }
            );

            for (Pair<String, String> entityPair : entities) {
                Label separator = new Label("<hr/>");
                separator.setContentMode(ContentMode.HTML);

                contentLayout.addComponent(separator);

                GridLayout grid = new GridLayout(2, 1);

                Label entityLabel = new Label(entityPair.getSecond());
                entityLabel.setStyleName("h2");
                entityLabel.setWidth(200, Sizeable.Unit.PIXELS);
                grid.addComponent(entityLabel, 0, 0);

                VerticalLayout instancesLayout = new VerticalLayout();
                displayInstances(entityPair.getFirst(), instancesLayout);
                grid.addComponent(instancesLayout, 1, 0);

                contentLayout.addComponent(grid);
            }
        }
    }

    private void displayInstances(String entityName, VerticalLayout instancesLayout) {
        List<SearchResult.Entry> entries = searchResult.getEntries(entityName);
//        Collections.sort(entries);

        for (SearchResult.Entry entry : entries) {
            HorizontalLayout instanceLayout = new HorizontalLayout();

            Button instanceBtn = new CubaButton(entry.getCaption());
            instanceBtn.setStyleName(BaseTheme.BUTTON_LINK);
            instanceBtn.addStyleName("fts-found-instance");
            instanceBtn.addClickListener(new InstanceClickListener(entityName, entry.getId()));

            instanceLayout.addComponent(instanceBtn);
            instanceLayout.setComponentAlignment(instanceBtn, com.vaadin.ui.Alignment.MIDDLE_LEFT);

            instancesLayout.addComponent(instanceLayout);

            SearchResult.HitInfo hi = searchResult.getHitInfo(entry.getId());
            if (hi != null) {
                List<String> list = new ArrayList<>(hi.getHits().size());
                for (Map.Entry<String, String> hitEntry : hi.getHits().entrySet()) {
                    String hitProperty = hitEntry.getKey();
                    list.add(service.getHitPropertyCaption(entityName, hitProperty) + ": " + hitEntry.getValue());
                }
                Collections.sort(list);

                for (String caption : list) {
                    HorizontalLayout hitLayout = new HorizontalLayout();
                    hitLayout.addStyleName("fts-hit");

                    Label hitLabel = new Label(caption);
                    hitLabel.setContentMode(ContentMode.HTML);
                    hitLabel.addStyleName("fts-hit");

                    hitLayout.addComponent(hitLabel);
                    hitLayout.setComponentAlignment(hitLabel, com.vaadin.ui.Alignment.MIDDLE_LEFT);

                    instancesLayout.addComponent(hitLayout);
                }
            }
        }
        if (!searchResult.getIds(entityName).isEmpty()) {
            displayMore(entityName, instancesLayout);
        }
    }

    protected void displayMore(String entityName, VerticalLayout instancesLayout) {
        HorizontalLayout instanceLayout = new HorizontalLayout();

        Button instanceBtn = new CubaButton(getMessage("more"));
        instanceBtn.setStyleName(BaseTheme.BUTTON_LINK);
        instanceBtn.addStyleName("fts-found-instance");
        instanceBtn.addClickListener(new MoreClickListener(entityName, instancesLayout));

        instanceLayout.addComponent(instanceBtn);
        instanceLayout.setComponentAlignment(instanceBtn, com.vaadin.ui.Alignment.MIDDLE_LEFT);

        instancesLayout.addComponent(instanceLayout);
    }

    private class InstanceClickListener implements Button.ClickListener {

        private String entityName;
        private UUID entityId;

        public InstanceClickListener(String entityName, UUID entityId) {
            this.entityName = entityName;
            this.entityId = entityId;
        }

        @Override
        public void buttonClick(Button.ClickEvent event) {
            MetaClass metaClass = metadata.getSession().getClass(entityName);
            Entity entity = reloadEntity(metaClass, entityId);

            AppWindow appWindow = App.getInstance().getAppWindow();
            AppWorkArea workArea = appWindow.getMainWindow().getWorkArea();

            if (workArea != null) {
                WindowManager.OpenType openType = AppWorkArea.Mode.TABBED == workArea.getMode() ?
                        WindowManager.OpenType.NEW_TAB : WindowManager.OpenType.THIS_TAB;

                WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
                openEditor(windowConfig.getEditorScreenId(metaClass), entity, openType);
            } else {
                throw new IllegalStateException("Application does not have any configured work area");
            }
        }
    }

    protected Entity reloadEntity(MetaClass metaClass, UUID entityId) {
        LoadContext lc = new LoadContext(metaClass);
        lc.setView(View.MINIMAL);
        lc.setId(entityId);
        return getDsContext().getDataSupplier().load(lc);
    }

    protected class MoreClickListener implements Button.ClickListener {

        private String entityName;
        private VerticalLayout instancesLayout;

        public MoreClickListener(String entityName, VerticalLayout instancesLayout) {
            this.entityName = entityName;
            this.instancesLayout = instancesLayout;
        }

        @Override
        public void buttonClick(Button.ClickEvent event) {
            searchResult = service.expandResult(searchResult, entityName);

            instancesLayout.removeAllComponents();
            displayInstances(entityName, instancesLayout);
        }
    }
}