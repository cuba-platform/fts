/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.app.QueryResultsService;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.filter.FtsFilterHelper;
import com.haulmont.cuba.gui.components.filter.condition.CustomCondition;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.fts.global.HitInfo;
import com.haulmont.fts.global.SearchResult;
import com.haulmont.fts.global.SearchResultEntry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(FtsFilterHelper.NAME)
public class FtsFilterHelperBean implements FtsFilterHelper {

    @Inject
    protected FtsService ftsService;

    @Inject
    protected QueryResultsService queryResultsService;

    @Inject
    protected UserSessionSource userSessionSource;

    @Inject
    protected Metadata metadata;

    @Override
    public boolean isEntityIndexed(String entityName) {
        return ftsService.isEntityIndexed(entityName);
    }

    @Override
    public FtsSearchResult search(String searchTerm, String entityName) {
        List<String> entityNames = ftsService.collectEntityHierarchyNames(entityName);
        SearchResult searchResult = ftsService.search(searchTerm.toLowerCase(), entityNames);
        List ids = new ArrayList<>();
        Map<Object, String> resultHitInfos = new HashMap<>();
        for (String entity : searchResult.getEntityNames()) {
            for (SearchResultEntry entry : searchResult.getEntries(entity)) {
                ids.add(entry.getId());
                HitInfo hitInfo = searchResult.getHitInfo(entry.getId(), entityName);
                if (hitInfo != null) {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> hitEntry : hitInfo.getHits().entrySet()) {
                        sb.append(ftsService.getHitPropertyCaption(entity, hitEntry.getKey()))
                                .append(": ")
                                .append(hitEntry.getValue())
                                .append("<br/>");

                    }
                    resultHitInfos.put(entry.getId(), sb.toString());
                }
            }
        }
        int queryKey = getNextQueryKey();
        queryResultsService.insert(queryKey, ids);

        FtsSearchResult ftsSearchResult = new FtsSearchResult();
        ftsSearchResult.setQueryKey(queryKey);
        ftsSearchResult.setHitInfos(resultHitInfos);

        return ftsSearchResult;
    }

    @Override
    public CustomCondition createFtsCondition(String entityName) {
        CustomCondition condition = new CustomCondition();
        condition.setWhere(createFtsWhereClause(entityName));
        condition.setUnary(true);
        return condition;
    }

    @Override
    public String createFtsWhereClause(String entityName) {
        return createFtsWhereClause(entityName, QUERY_KEY_PARAM_NAME, SESSION_ID_PARAM_NAME);
    }

    @Override
    public String createFtsWhereClause(String entityName, String queryKeyParamName, String sessionIdParamName) {
        MetaClass metaClass = metadata.getClassNN(entityName);
        MetaProperty primaryKeyForFts = ftsService.getPrimaryKeyPropertyForFts(metaClass);
        Class type = primaryKeyForFts.getJavaType();
        String entityIdField;
        if (Long.class.equals(type)) {
            entityIdField = "longEntityId";
        } else if (Integer.class.equals(type)) {
            entityIdField = "intEntityId";
        } else if (String.class.equals(type)) {
            entityIdField = "stringEntityId";
        } else {
            entityIdField = "entityId";
        }
        return String.format("exists (select qr from sys$QueryResult qr where qr.%s = {E}.%s and qr.queryKey = :custom$%s and qr.sessionId = :custom$%s)",
                entityIdField, primaryKeyForFts.getName(), queryKeyParamName, sessionIdParamName);
    }

    protected int getNextQueryKey() {
        UserSession userSession = userSessionSource.getUserSession();
        Integer queryKey = userSession.getAttribute("_queryKey");
        if (queryKey == null)
            queryKey = 1;
        else
            queryKey++;
        userSession.setAttribute("_queryKey", queryKey);
        return queryKey;
    }
}
