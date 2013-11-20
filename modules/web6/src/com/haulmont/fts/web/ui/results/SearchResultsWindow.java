/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.web.ui.results;

import com.haulmont.bali.datastruct.Pair;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.Session;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppWindow;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.SearchResult;
import com.vaadin.terminal.Sizeable;
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

    public SearchResultsWindow() {
    }

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
                entityLabel.setWidth(200, Sizeable.UNITS_PIXELS);
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

            Button instanceBtn = new Button(entry.getCaption());
            instanceBtn.setStyleName(BaseTheme.BUTTON_LINK);
            instanceBtn.addStyleName("fts-found-instance");
            instanceBtn.addListener(new InstanceClickListener(entityName, entry.getId()));

            instanceLayout.addComponent(instanceBtn);
            instanceLayout.setComponentAlignment(instanceBtn, com.vaadin.ui.Alignment.MIDDLE_LEFT);

            instancesLayout.addComponent(instanceLayout);

            SearchResult.HitInfo hi = searchResult.getHitInfo(entry.getId());
            if (hi != null) {
                List<String> list = new ArrayList<>(hi.getHits().size());
                for (Map.Entry<String, String> hitEntry : hi.getHits().entrySet()) {
                    String hitProperty = hitEntry.getKey();
                    list.add(getHitPropertyCaption(entityName, hitProperty) + ": " + hitEntry.getValue());
                }
                Collections.sort(list);

                for (String caption : list) {
                    HorizontalLayout hitLayout = new HorizontalLayout();
                    hitLayout.addStyleName("fts-hit");

                    Label hitLabel = new Label(caption);
                    hitLabel.setContentMode(Label.CONTENT_XHTML);
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

        Button instanceBtn = new Button(getMessage("more"));
        instanceBtn.setStyleName(BaseTheme.BUTTON_LINK);
        instanceBtn.addStyleName("fts-found-instance");
        instanceBtn.addListener(new MoreClickListener(entityName, instancesLayout));

        instanceLayout.addComponent(instanceBtn);
        instanceLayout.setComponentAlignment(instanceBtn, com.vaadin.ui.Alignment.MIDDLE_LEFT);

        instancesLayout.addComponent(instanceLayout);
    }

    private String getHitPropertyCaption(String entityName, String hitProperty) {
        String[] parts = hitProperty.split("\\.");
        if (parts.length == 1) {
            MetaClass metaClass = metadata.getSession().getClass(entityName);
            if (metaClass == null)
                return hitProperty;

            MetaProperty metaProperty = metaClass.getProperty(hitProperty);
            if (metaProperty == null)
                return hitProperty;

            return messages.getTools().getPropertyCaption(metaProperty);
        } else {
            String linkEntityName = parts[0];
            MetaClass metaClass = metadata.getSession().getClass(linkEntityName);
            if (metaClass == null)
                return hitProperty;

            if (metaClass == fileMetaClass && parts[1].equals(FTS.FILE_CONT_PROP)) {
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < parts.length; i++) {
                    sb.append(parts[i]);
                    if (i < parts.length - 1)
                        sb.append(".");
                }
                return messages.formatMessage(getClass(), "fileContent", sb.toString());
            }

            MetaProperty metaProperty = metaClass.getProperty(parts[1]);
            if (metaProperty == null)
                return hitProperty;

            return messages.getTools().getEntityCaption(metaClass) + "."
                    + messages.getTools().getPropertyCaption(metaProperty);
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

            LoadContext lc = new LoadContext(metadata.getSession().getClass(entityName));
            lc.setView(View.MINIMAL);
            lc.setId(entityId);
            Entity entity = getDsContext().getDataSupplier().load(lc);

            WindowManager.OpenType openType = AppWindow.Mode.TABBED.equals(App.getInstance().getAppWindow().getMode()) ?
                    WindowManager.OpenType.NEW_TAB : WindowManager.OpenType.THIS_TAB;
            openEditor(windowId, entity, openType);
        }
    }

    protected class MoreClickListener implements Button.ClickListener {

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
