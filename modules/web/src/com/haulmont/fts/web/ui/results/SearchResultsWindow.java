/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
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
 * Screen that displays Full-Text Search results.
 * <p>
 * XML-descriptor: com/haulmont/fts/web/ui/results/search-results.xml
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
            initNoSearchTerm();
        } else {
            initSearchResults(params, searchTerm);
        }
    }

    protected void initNoSearchTerm() {
        setCaption(getMessage("caption"));
        Label label = new Label(getMessage("noSearchTerm"));
        label.setStyleName("h2");
        contentLayout.addComponent(label);
    }

    protected void initSearchResults(Map<String, Object> params, String searchTerm) {
        searchTerm = searchTerm.trim();
        setCaption(getMessage("caption") + ": " + searchTerm);

        searchResult = (SearchResult) params.get("searchResult");
        if (searchResult == null)
            searchResult = service.search(searchTerm.toLowerCase());

        paintResult(searchResult);
    }

    protected void paintResult(SearchResult searchResult) {
        if (searchResult.isEmpty()) {
            contentLayout.addComponent(createNotFoundLabel());
        } else {
            Session session = metadata.getSession();

            List<Pair<String, String>> entities = new ArrayList<>();
            for (String entityName : searchResult.getEntities()) {
                entities.add(new Pair<>(
                        entityName,
                        messages.getTools().getEntityCaption(session.getClass(entityName))
                ));
            }
            entities.sort(Comparator.comparing(Pair::getSecond));

            for (Pair<String, String> entityPair : entities) {
                contentLayout.addComponent(createSeparator());

                GridLayout grid = new GridLayout(2, 1);

                grid.addComponent(createEntityLabel(entityPair.getSecond()), 0, 0);

                VerticalLayout instancesLayout = new VerticalLayout();
                displayInstances(entityPair.getFirst(), instancesLayout);
                grid.addComponent(instancesLayout, 1, 0);

                contentLayout.addComponent(grid);
            }
        }
    }

    protected Label createNotFoundLabel() {
        Label label = new Label(getMessage("notFound"));
        label.setStyleName("h2");
        return label;
    }

    protected Label createSeparator() {
        Label separator = new Label("<hr/>");
        separator.setContentMode(ContentMode.HTML);
        return separator;
    }

    protected Label createEntityLabel(String caption) {
        Label entityLabel = new Label(caption);
        entityLabel.setStyleName("h2");
        entityLabel.setWidth(200, Sizeable.Unit.PIXELS);
        return entityLabel;
    }

    protected void displayInstances(String entityName, VerticalLayout instancesLayout) {
        List<SearchResult.Entry> entries = searchResult.getEntries(entityName);

        for (SearchResult.Entry entry : entries) {
            HorizontalLayout instanceLayout = new HorizontalLayout();

            Button instanceBtn = createInstanceButton(entityName, entry);
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

                    Label hitLabel = createHitLabel(caption);
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

    protected Button createInstanceButton(String entityName, SearchResult.Entry entry) {
        Button instanceBtn = new CubaButton(entry.getCaption());
        instanceBtn.setStyleName(BaseTheme.BUTTON_LINK);
        instanceBtn.addStyleName("fts-found-instance");
        instanceBtn.addClickListener(event -> onInstanceClick(entityName, entry));
        return instanceBtn;
    }

    protected void onInstanceClick(String entityName, SearchResult.Entry entry) {
        TopLevelWindow appWindow = App.getInstance().getTopLevelWindow();
        if (appWindow instanceof HasWorkArea) {
            AppWorkArea workArea = ((HasWorkArea) appWindow).getWorkArea();

            if (workArea != null) {
                WindowManager.OpenType openType = AppWorkArea.Mode.TABBED == workArea.getMode() ?
                        WindowManager.OpenType.NEW_TAB : WindowManager.OpenType.THIS_TAB;

                openEntityWindow(entry, entityName, openType);
            } else {
                throw new IllegalStateException("Application does not have any configured work area");
            }
        }
    }

    protected Label createHitLabel(String caption) {
        Label hitLabel = new Label(caption);
        hitLabel.setContentMode(ContentMode.HTML);
        hitLabel.addStyleName("fts-hit");
        return hitLabel;
    }

    protected void openEntityWindow(SearchResult.Entry entry, String entityName, WindowManager.OpenType openType) {
        WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
        MetaClass metaClass = metadata.getSession().getClass(entityName);
        Entity entity = reloadEntity(metaClass, entry.getId());
        openEditor(windowConfig.getEditorScreenId(metaClass), entity, openType);
    }

    protected void displayMore(String entityName, VerticalLayout instancesLayout) {
        HorizontalLayout layout = new HorizontalLayout();

        Button moreBtn = createMoreButton(entityName, instancesLayout);
        layout.addComponent(moreBtn);
        layout.setComponentAlignment(moreBtn, com.vaadin.ui.Alignment.MIDDLE_LEFT);

        instancesLayout.addComponent(layout);
    }

    protected Button createMoreButton(String entityName, VerticalLayout instancesLayout) {
        Button instanceBtn = new CubaButton(getMessage("more"));
        instanceBtn.setStyleName(BaseTheme.BUTTON_LINK);
        instanceBtn.addStyleName("fts-found-instance");
        instanceBtn.addClickListener(event -> onMoreClick(entityName, instancesLayout));
        return instanceBtn;
    }

    protected void onMoreClick(String entityName, VerticalLayout instancesLayout) {
        searchResult = service.expandResult(searchResult, entityName);

        instancesLayout.removeAllComponents();
        displayInstances(entityName, instancesLayout);
    }

    /**
     * For entities with composite keys there will be a value of the 'uuid' property in the {@code entityId} parameter.
     */
    protected Entity reloadEntity(MetaClass metaClass, Object entityId) {
        String ftsPrimaryKeyName = service.getPrimaryKeyPropertyForFts(metaClass).getName();
        String queryStr = String.format("select e from %s e where e.%s = :id", metaClass.getName(), ftsPrimaryKeyName);
        LoadContext lc = new LoadContext(metaClass)
                .setView(View.MINIMAL)
                .setQuery(LoadContext.createQuery(queryStr).setParameter("id", entityId));
        List list = getDsContext().getDataSupplier().loadList(lc);
        return list.isEmpty() ? null : (Entity) list.get(0);
    }
}