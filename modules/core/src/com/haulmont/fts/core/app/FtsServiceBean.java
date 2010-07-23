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
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Locator;
import com.haulmont.cuba.core.PersistenceProvider;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.FtsConfig;
import com.haulmont.cuba.core.global.MetadataProvider;
import com.haulmont.cuba.core.global.View;
import com.haulmont.fts.app.FtsService;
import com.haulmont.fts.core.sys.EntityInfo;
import com.haulmont.fts.core.sys.LuceneSearcher;
import com.haulmont.fts.global.SearchResult;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service(FtsService.NAME)
public class FtsServiceBean implements FtsService {

    private LuceneSearcher searcher;

    private FtsManagerAPI manager;

    @Inject
    public void setManager(FtsManagerAPI manager) {
        this.manager = manager;
    }

    private LuceneSearcher getSearcher() {
        if (searcher == null) {
            synchronized (this) {
                if (searcher == null) {
                    searcher = new LuceneSearcher(manager.getDirectory());
                }
            }
        }
        return searcher;
    }

    public SearchResult search(String searchTerm) {
        if (searcher != null && !searcher.isCurrent())
            searcher = null;

        int maxResults = ConfigProvider.getConfig(FtsConfig.class).getMaxSearchResults();
        SearchResult result = new SearchResult();

        List<EntityInfo> allFieldResults = getSearcher().searchAllField(searchTerm, maxResults);
        if (!allFieldResults.isEmpty()) {
            Transaction tx = Locator.createTransaction();
            try {
                for (EntityInfo entityInfo : allFieldResults) {
                    result.addEntry(
                            entityInfo.getName(),
                            new SearchResult.Entry(entityInfo.getId(), getEntityCaption(entityInfo))
                    );
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
                        for (EntityInfo linksFieldResult : linksFieldResults) {
                            result.addEntry(
                                    linksFieldResult.getName(),
                                    new SearchResult.Entry(linksFieldResult.getId(), getEntityCaption(linksFieldResult))
                            );
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

    private String getEntityCaption(EntityInfo entityInfo) {
        Class javaClass = MetadataProvider.getSession().getClass(entityInfo.getName()).getJavaClass();

        EntityManager em = PersistenceProvider.getEntityManager();
        em.setView(MetadataProvider.getViewRepository().getView(javaClass, View.MINIMAL));
        Entity entity = em.find(javaClass, entityInfo.getId());

        return ((Instance) entity).getInstanceName();
    }
}
