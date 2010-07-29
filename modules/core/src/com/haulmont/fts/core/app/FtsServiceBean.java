/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.07.2010 15:47:51
 *
 * $Id$
 */
package com.haulmont.fts.core.app;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.FtsConfig;
import com.haulmont.cuba.core.global.MetadataProvider;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.core.sys.EntityInfo;
import com.haulmont.fts.core.sys.LuceneSearcher;
import com.haulmont.fts.global.SearchResult;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@Service(FtsService.NAME)
public class FtsServiceBean implements FtsService {

    private LuceneSearcher searcher;

    private FtsManagerAPI manager;

    private FtsConfig config;

    @Inject
    public void setManager(FtsManagerAPI manager) {
        this.manager = manager;
    }

    @Inject
    public void setConfigProvider(ConfigProvider configProvider) {
        config = configProvider.doGetConfig(FtsConfig.class);
    }

    private LuceneSearcher getSearcher() {
        if (searcher == null) {
            synchronized (this) {
                if (searcher == null) {
                    searcher = new LuceneSearcher(manager.getDirectory(), config.getStoreContentInIndex());
                }
            }
        }
        return searcher;
    }

    public SearchResult search(String searchTerm) {
        if (searcher != null && !searcher.isCurrent())
            searcher = null;

        int maxResults = config.getMaxSearchResults();
        SearchResult result = new SearchResult(searchTerm);

        List<EntityInfo> allFieldResults = getSearcher().searchAllField(searchTerm, maxResults);
        if (!allFieldResults.isEmpty()) {
            Transaction tx = Locator.createTransaction();
            try {
                for (EntityInfo entityInfo : allFieldResults) {
                    if (!manager.showInResults(entityInfo.getName()))
                        continue;

                    if (result.getEntriesCount(entityInfo.getName()) < config.getSearchResultsBatchSize()) {
                        SearchResult.Entry entry = createEntry(entityInfo.getName(), entityInfo.getId());
                        if (entry != null) {
                            result.addEntry(entityInfo.getName(), entry);
                        }
                    } else {
                        if (!result.hasEntry(entityInfo.getName(), entityInfo.getId())) {
                            result.addId(entityInfo.getName(), entityInfo.getId());
                        }
                    }
                    result.addHit(entityInfo.getId(), entityInfo.getText(), null);
                }
                tx.commit();
            } finally {
                tx.end();
            }

            for (EntityInfo entityInfo : allFieldResults) {
                List<EntityInfo> linksFieldResults = getSearcher().searchLinksField(entityInfo.getId(), maxResults);
                if (!linksFieldResults.isEmpty()) {
                    tx = Locator.createTransaction();
                    try {
                        for (EntityInfo linkEntityInfo : linksFieldResults) {
                            if (!manager.showInResults(linkEntityInfo.getName()))
                                continue;
                            
                            if (result.getEntriesCount(linkEntityInfo.getName()) < config.getSearchResultsBatchSize()) {
                                SearchResult.Entry entry = createEntry(linkEntityInfo.getName(), linkEntityInfo.getId());
                                if (entry != null) {
                                    result.addEntry(linkEntityInfo.getName(), entry);
                                }
                            } else {
                                if (!result.hasEntry(linkEntityInfo.getName(), linkEntityInfo.getId())) {
                                    result.addId(linkEntityInfo.getName(), linkEntityInfo.getId());
                                }
                            }
                            result.addHit(linkEntityInfo.getId(), entityInfo.getText(), entityInfo.getName());
                        }
                        tx.commit();
                    } finally {
                        tx.end();
                    }
                }
            }
        }

        return result;
    }

    public SearchResult expandResult(SearchResult result, String entityName) {
        int max = result.getEntriesCount(entityName) + config.getSearchResultsBatchSize();

        Transaction tx = Locator.createTransaction();
        try {
            for (UUID id : result.getIds(entityName)) {
                if (result.getEntriesCount(entityName) > max)
                    break;

                SearchResult.Entry entry = createEntry(entityName, id);
                if (entry != null) {
                    result.addEntry(entityName, entry);
                }

                result.removeId(entityName, id);
            }
            tx.commit();
        } finally {
            tx.end();
        }
        return result;
    }

    private SearchResult.Entry createEntry(String entityName, UUID entityId) {
        MetaClass metaClass = MetadataProvider.getSession().getClass(entityName);

        if (!SecurityProvider.currentUserSession().isEntityOpPermitted(metaClass, EntityOp.READ))
            return null;

        EntityManager em = PersistenceProvider.getEntityManager();

        Query query = em.createQuery("select e from " + entityName + " e where e.id = :id");
        SecurityProvider.applyConstraints(query, entityName);

        query.setParameter("id", entityId);

        query.setView(MetadataProvider.getViewRepository().getView(metaClass.getJavaClass(), View.MINIMAL));

        List<Entity> list = query.getResultList();
        if (list.isEmpty())
            return null;

        String entityCaption = ((Instance) list.get(0)).getInstanceName();
        return new SearchResult.Entry(entityId, entityCaption);
    }
}
