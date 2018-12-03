/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UuidProvider;
import com.haulmont.fts.core.app.FtsManagerAPI;
import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.FtsConfig;
import com.haulmont.fts.global.ValueFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

import static com.haulmont.fts.core.sys.LuceneConstants.*;

@Component(LuceneSearcher.NAME)
public class LuceneSearcherBean implements LuceneSearcher {

    @Inject
    protected FtsConfig ftsConfig;

    @Inject
    protected IndexSearcherProvider indexSearcherProvider;

    @Override
    public List<EntityInfo> searchAllField(String searchTerm, int firstResult, int maxResults) {
        Set<EntityInfo> set = new LinkedHashSet<>();
        Query query = createQueryForAllFieldSearch(searchTerm);
        IndexSearcher searcher = null;
        try {
            searcher = indexSearcherProvider.acquireIndexSearcher();
            TopScoreDocCollector collector = TopScoreDocCollector.create(firstResult + maxResults);
            searcher.search(query, collector);
            TopDocs topDocs = collector.topDocs(firstResult, maxResults);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                String strEntityId = doc.getField(FLD_ID).stringValue();
                String text = ftsConfig.getStoreContentInIndex() ? doc.getField(FLD_ALL).stringValue() : null;
                EntityInfo entityInfo = new EntityInfo(entityName, parseIdFromString(strEntityId, entityName), text, false);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException("Search error", e);
        } finally {
            if (searcher != null)
                indexSearcherProvider.releaseIndexSearcher(searcher);
        }
        return new ArrayList<>(set);
    }

    @Override
    public List<EntityInfo> searchAllField(String searchTerm, Collection<String> entityNames) {
        Set<EntityInfo> set = new LinkedHashSet<>();
        Query query = createQueryForAllFieldSearch(searchTerm, entityNames);
        IndexSearcher searcher = null;
        try {
            searcher = indexSearcherProvider.acquireIndexSearcher();
            AllDocsCollector collector = new AllDocsCollector();
            searcher.search(query, collector);
            for (Integer docId : collector.getDocIds()) {
                Document doc = searcher.doc(docId);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                String strEntityId = doc.getField(FLD_ID).stringValue();
                String text = ftsConfig.getStoreContentInIndex() ? doc.getField(FLD_ALL).stringValue() : null;
                EntityInfo entityInfo = new EntityInfo(entityName, parseIdFromString(strEntityId, entityName), text, false);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException("Search error", e);
        } finally {
            if (searcher != null)
                indexSearcherProvider.releaseIndexSearcher(searcher);
        }
        return new ArrayList<>(set);
    }


    protected Query createQueryForAllFieldSearch(String searchTerm) {
        Query query;

        if (searchTerm.startsWith("\"") && searchTerm.endsWith("\"")) {
            searchTerm = searchTerm.substring(1, searchTerm.length() - 1);
            PhraseQuery.Builder builder = new PhraseQuery.Builder();
            FTS.Tokenizer tokenizer = new FTS.Tokenizer(searchTerm);
            while (tokenizer.hasMoreTokens()) {
                builder.add(new Term(FLD_ALL, tokenizer.nextToken()));
            }
            query = builder.build();
        } else {
            ValueFormatter valueFormatter = new ValueFormatter();
            String[] strings = searchTerm.split("\\s");
            if (strings.length == 1) {
                query = createQuery(searchTerm, valueFormatter);
            } else {
                BooleanQuery.Builder builder = new BooleanQuery.Builder();
                for (String string : strings) {
                    if (StringUtils.isNotEmpty(string)) {
                        Query q = createQuery(string, valueFormatter);
                        builder.add(q, BooleanClause.Occur.SHOULD);
                    }
                }
                query = builder.build();
            }
        }

        return query;
    }

    protected Query createQueryForAllFieldSearch(String searchTerm, Collection<String> entityNames) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        Query queryForAllFieldSearch = createQueryForAllFieldSearch(searchTerm);

        BooleanQuery.Builder entityNamesQueryBuilder = new BooleanQuery.Builder();
        for (String entityName : entityNames) {
            Term term = new Term(FLD_ENTITY, entityName);
            entityNamesQueryBuilder.add(new TermQuery(term), BooleanClause.Occur.SHOULD);
        }
        BooleanQuery entityNamesQuery = entityNamesQueryBuilder.build();

        builder.add(queryForAllFieldSearch, BooleanClause.Occur.MUST);
        builder.add(entityNamesQuery, BooleanClause.Occur.MUST);
        return builder.build();
    }

    protected Query createQueryForLinksFieldSearch(Object id, List<String> entityNames) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        TermQuery idQuery = new TermQuery(new Term(FLD_LINKS, id.toString()));
        BooleanQuery.Builder entityNamesQueryBuilder = new BooleanQuery.Builder();
        for (String entityName : entityNames) {
            Term term = new Term(FLD_ENTITY, entityName);
            entityNamesQueryBuilder.add(new TermQuery(term), BooleanClause.Occur.SHOULD);
        }
        builder.add(idQuery, BooleanClause.Occur.MUST);
        builder.add(entityNamesQueryBuilder.build(), BooleanClause.Occur.MUST);
        return builder.build();
    }

    protected Query createQuery(String searchStr, ValueFormatter valueFormatter) {
        Query query;
        String s = valueFormatter.guessTypeAndFormat(searchStr);
        if (s.startsWith("*")) {
            if (!s.endsWith("*"))
                s = s + "*";
            Term term = new Term(FLD_ALL, s);
            query = new WildcardQuery(term);
        } else {
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            Term term = new Term(FLD_ALL, s);
            MorphologyNormalizer morphologyNormalizer = new MorphologyNormalizer();
            Term morphologyTerm = new Term(FLD_MORPHOLOGY_ALL, morphologyNormalizer.getAnyNormalForm(s));
            booleanQueryBuilder.add(new PrefixQuery(term), BooleanClause.Occur.SHOULD);
            booleanQueryBuilder.add(new TermQuery(morphologyTerm), BooleanClause.Occur.SHOULD);
            query = booleanQueryBuilder.build();
        }
        return query;
    }

    protected Object parseIdFromString(String strId, String entityName) {
        Metadata metadata = AppBeans.get(Metadata.class);
        MetaClass metaClass = metadata.getSession().getClassNN(entityName);
        MetaProperty primaryKey = AppBeans.get(FtsManagerAPI.class).getPrimaryKeyPropertyForFts(metaClass);
        if (primaryKey != null) {
            Class type = primaryKey.getJavaType();
            if (UUID.class.equals(type)) {
                return UuidProvider.fromString(strId);
            } else if (Long.class.equals(type)) {
                return Long.valueOf(strId);
            } else if (Integer.class.equals(type)) {
                return Integer.valueOf(strId);
            } else if (String.class.equals(type)) {
                return strId;
            } else {
                throw new IllegalStateException(
                        String.format("Unsupported primary key type: %s for %s", type.getSimpleName(), entityName));
            }
        } else {
            throw new IllegalStateException(
                    String.format("Primary key not found for %s", entityName));
        }
    }

    @Override
    public List<EntityInfo> searchLinksField(Object id, int firstResult, int maxResults) {
        Set<EntityInfo> set = new LinkedHashSet<>();
        Term term = new Term(FLD_LINKS, id.toString());
        Query termQuery = new TermQuery(term);
        IndexSearcher searcher = indexSearcherProvider.acquireIndexSearcher();
        try {
            TopScoreDocCollector collector = TopScoreDocCollector.create(firstResult + maxResults);
            searcher.search(termQuery, collector);
            TopDocs topDocs = collector.topDocs(firstResult, maxResults);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                String strEntityId = doc.getField(FLD_ID).stringValue();
                EntityInfo entityInfo = new EntityInfo(entityName, parseIdFromString(strEntityId, entityName), null, true);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException("Search error", e);
        } finally {
            indexSearcherProvider.releaseIndexSearcher(searcher);
        }
        return new ArrayList<>(set);
    }

    @Override
    public List<EntityInfo> searchLinksField(Object id, List<String> entityNames) {
        Set<EntityInfo> set = new LinkedHashSet<>();
        Query query = createQueryForLinksFieldSearch(id, entityNames);
        IndexSearcher searcher = indexSearcherProvider.acquireIndexSearcher();
        try {
            AllDocsCollector collector = new AllDocsCollector();
            searcher.search(query, collector);
            for (Integer docId : collector.getDocIds()) {
                Document doc = searcher.doc(docId);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                String strEntityId = doc.getField(FLD_ID).stringValue();
                String text = ftsConfig.getStoreContentInIndex() ? doc.getField(FLD_ALL).stringValue() : null;
                EntityInfo entityInfo = new EntityInfo(entityName, parseIdFromString(strEntityId, entityName), text, true);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException("Search error", e);
        } finally {
            indexSearcherProvider.releaseIndexSearcher(searcher);
        }
        return new ArrayList<>(set);
    }
}
