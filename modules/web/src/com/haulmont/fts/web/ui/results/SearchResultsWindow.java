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

import com.google.common.collect.EvictingQueue;
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
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.mainwindow.AppWorkArea;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.web.App;
import com.haulmont.fts.app.FtsSearchOption;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.global.FtsConfig;
import com.haulmont.fts.global.HitInfo;
import com.haulmont.fts.global.SearchResult;
import com.haulmont.fts.global.SearchResultEntry;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.*;

/**
 * Screen that displays Full-Text Search results.
 * <p>
 * XML-descriptor: com/haulmont/fts/web/ui/results/search-results.xml
 */
public class SearchResultsWindow extends AbstractWindow {

    @Inject
    protected BoxLayout navigationBox;

    @Inject
    protected ScrollBoxLayout contentBox;

    @Inject
    protected FtsService ftsService;

    @Inject
    protected Metadata metadata;

    @Inject
    protected ComponentsFactory componentsFactory;

    @Inject
    protected FtsConfig ftsConfig;

    protected MetaClass fileMetaClass;


    protected Page currentPage;
    protected Queue<Page> pages;

    protected static class Page {
        protected int pageNumber;
        protected boolean lastPage;
        protected SearchResult searchResult;

        public Page(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public void setSearchResult(SearchResult searchResult) {
            this.searchResult = searchResult;
        }

        public SearchResult getSearchResult() {
            return searchResult;
        }

        public boolean isLastPage() {
            return lastPage;
        }

        public void setLastPage(boolean lastPage) {
            this.lastPage = lastPage;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public String getDisplayedPageNumber() {
            return String.valueOf(pageNumber + 1);
        }
    }

    @Override
    public void init(Map<String, Object> params) {
        fileMetaClass = metadata.getSession().getClassNN(FileDescriptor.class);

        String searchTerm = (String) params.get("searchTerm");
        if (StringUtils.isBlank(searchTerm)) {
            initNoSearchTerm();
        } else {
            initSearchResults(params, searchTerm);
        }
    }

    protected void initNoSearchTerm() {
        setCaption(getMessage("caption"));
        Label<String> label = componentsFactory.createComponent(Label.class);
        label.setValue(getMessage("noSearchTerm"));
        label.setStyleName("h2");
        contentBox.add(label);
    }

    protected void initSearchResults(Map<String, Object> params, String searchTerm) {
        searchTerm = searchTerm.trim();
        setCaption(getMessage("caption") + ": " + searchTerm);

        SearchResult searchResult = (SearchResult) params.get("searchResult");
        //noinspection UnstableApiUsage
        pages = EvictingQueue.create(ftsConfig.getPagesCount());
        currentPage = new Page(0);
        if (searchResult == null) {
            searchResult = ftsService.search(searchTerm.toLowerCase(), null, FtsSearchOption.POPULATE_HIT_INFOS);
        }
        currentPage.setSearchResult(searchResult);
        pages.add(currentPage);

        paintResult(currentPage);
        paintNavigationControls(pages);
    }

    protected void paintResult(Page page) {
        contentBox.removeAll();
        SearchResult searchResult = page.getSearchResult();
        if (searchResult.isEmpty()) {
            contentBox.add(createNotFoundLabel());
        } else {
            Session session = metadata.getSession();

            List<Pair<String, String>> entities = new ArrayList<>();
            for (String entityName : searchResult.getEntityNames()) {
                entities.add(new Pair<>(
                        entityName,
                        messages.getTools().getEntityCaption(session.getClass(entityName))
                ));
            }
            entities.sort(Comparator.comparing(Pair::getSecond));

            for (Pair<String, String> entityPair : entities) {
                CssLayout container = componentsFactory.createComponent(CssLayout.class);
                container.setStyleName("c-fts-entities-container");
                container.setWidth("100%");

                CssLayout entityLabelLayout = componentsFactory.createComponent(CssLayout.class);
                entityLabelLayout.setStyleName("c-fts-entities-type");
                entityLabelLayout.add(createEntityLabel(entityPair.getSecond()));

                container.add(entityLabelLayout);

                CssLayout instancesLayout = componentsFactory.createComponent(CssLayout.class);
                instancesLayout.setWidth("100%");
                displayInstances(searchResult, entityPair.getFirst(), instancesLayout);
                container.add(instancesLayout);

                contentBox.add(container);
            }
        }
    }

    protected void paintNavigationControls(Queue<Page> pages) {
        navigationBox.removeAll();
        for (Page page : pages) {
            LinkButton pageButton = componentsFactory.createComponent(LinkButton.class);
            BaseAction action = new BaseAction("page_" + page.getPageNumber())
                    .withCaption(page.getDisplayedPageNumber())
                    .withHandler(e -> openPage(page));
            pageButton.setAction(action);
            if (page == currentPage) {
                pageButton.setStyleName("c-fts-current-page");
            } else {
                pageButton.setStyleName("c-fts-page");
            }
            navigationBox.add(pageButton);
        }

        boolean showNextPage = true;
        Page lastPage = getLastPage();
        if (lastPage != null && lastPage.getSearchResult() != null) {
            SearchResult lastSearchResult = lastPage.getSearchResult();
            showNextPage = lastSearchResult.getCount() != 0 && !lastPage.isLastPage();
        }

        if (showNextPage) {
            LinkButton nextPageButton = componentsFactory.createComponent(LinkButton.class);
            BaseAction action = new BaseAction("nextPage")
                    .withCaption(getMessage("nextPage"))
                    .withHandler(e -> openNextPage());
            nextPageButton.setAction(action);
            nextPageButton.setStyleName("c-fts-page");
            navigationBox.add(nextPageButton);
        }
    }

    protected void openPage(Page page) {
        currentPage = page;
        paintResult(page);
        paintNavigationControls(pages);
    }

    protected void openNextPage() {
        Page lastPage = getLastPage();
        if (lastPage != null) {
            SearchResult lastSearchResult = lastPage.getSearchResult();
            SearchResult searchResult = ftsService.search(lastSearchResult.getSearchTerm(), lastSearchResult.getQueryKey(), FtsSearchOption.POPULATE_HIT_INFOS);
            if (searchResult.getCount() == 0) {
                currentPage.setLastPage(true);
                paintNavigationControls(pages);
                showNotification(getMessage("notFound"), NotificationType.HUMANIZED);
            } else {
                currentPage = new Page(lastPage.getPageNumber() + 1);
                currentPage.setSearchResult(searchResult);
                pages.add(currentPage);
                paintResult(currentPage);
                paintNavigationControls(pages);
            }
        }
    }

    protected Page getLastPage() {
        Page lastPage = null;
        for (Page page : pages) {
            if (lastPage == null) {
                lastPage = page;
            } else {
                if (page.getPageNumber() > lastPage.getPageNumber()) {
                    lastPage = page;
                }
            }
        }
        return lastPage;
    }

    protected Label createNotFoundLabel() {
        Label label = componentsFactory.createComponent(Label.class);
        label.setValue(getMessage("notFound"));
        label.setStyleName("h2");
        return label;
    }

    protected Label createEntityLabel(String caption) {
        Label entityLabel = componentsFactory.createComponent(Label.class);
        entityLabel.setValue(caption);
        entityLabel.setStyleName("h2");
        entityLabel.setWidth("200px");
        return entityLabel;
    }

    protected void displayInstances(SearchResult searchResult, String entityName, CssLayout instancesLayout) {
        Set<SearchResultEntry> entries = searchResult.getEntriesByEntityName(entityName);

        for (SearchResultEntry entry : entries) {
            Button instanceBtn = createInstanceButton(entityName, entry);
            instanceBtn.setAlignment(Alignment.MIDDLE_LEFT);
            instanceBtn.addStyleName("c-fts-entity");

            instancesLayout.add(instanceBtn);

            List<String> list = new ArrayList<>(entry.getHitInfos().size());
            for (HitInfo hitInfo : entry.getHitInfos()) {
                list.add(ftsService.getHitPropertyCaption(entityName, hitInfo.getFieldName()) + ": " + hitInfo.getHighlightedText());
            }
            Collections.sort(list);

            for (String caption : list) {
                Label hitLabel = createHitLabel(caption);
                hitLabel.addStyleName("c-fts-hit");
                hitLabel.addStyleName("fts-hit");
                hitLabel.setAlignment(Alignment.MIDDLE_LEFT);
                instancesLayout.add(hitLabel);
            }
        }
    }

    protected Button createInstanceButton(String entityName, SearchResultEntry entry) {
        LinkButton instanceBtn = componentsFactory.createComponent(LinkButton.class);
        instanceBtn.setStyleName("fts-found-instance");

        BaseAction action = new BaseAction("instanceButton");
        action.withCaption(entry.getInstanceName());
        action.withHandler(e -> onInstanceClick(entityName, entry));

        instanceBtn.setAction(action);

        return instanceBtn;
    }

    protected void onInstanceClick(String entityName, SearchResultEntry entry) {
        Screen appWindow = App.getInstance().getTopLevelWindow().getFrameOwner();
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
        Label<String> hitLabel = componentsFactory.createComponent(Label.class);
        hitLabel.setValue(caption);
        hitLabel.setHtmlEnabled(true);
        hitLabel.addStyleName("fts-hit");
        return hitLabel;
    }

    protected void openEntityWindow(SearchResultEntry entry, String entityName, WindowManager.OpenType openType) {
        WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
        MetaClass metaClass = metadata.getSession().getClass(entityName);
        Entity entity = reloadEntity(metaClass, entry.getEntityInfo().getId());
        openEditor(windowConfig.getEditorScreenId(metaClass), entity, openType);
    }

    /**
     * For entities with composite keys there will be a value of the 'uuid' property in the {@code entityId} parameter.
     */
    protected Entity reloadEntity(MetaClass metaClass, Object entityId) {
        String ftsPrimaryKeyName = ftsService.getPrimaryKeyPropertyForFts(metaClass).getName();
        String queryStr = String.format("select e from %s e where e.%s = :id", metaClass.getName(), ftsPrimaryKeyName);
        LoadContext lc = new LoadContext(metaClass)
                .setView(View.MINIMAL)
                .setQuery(LoadContext.createQuery(queryStr).setParameter("id", entityId));
        List list = getDsContext().getDataSupplier().loadList(lc);
        return list.isEmpty() ? null : (Entity) list.get(0);
    }
}