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
import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.ValueFormatter;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.*;

public class LuceneSearcher extends Lucene {

    protected IndexSearcher searcher;

    protected boolean storeContentInIndex;

    public LuceneSearcher(Directory directory, boolean storeContentInIndex) {
        super(directory);
        this.storeContentInIndex = storeContentInIndex;
        try {
            searcher = new IndexSearcher(DirectoryReader.open(directory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isCurrent() {
        try {
            return ((DirectoryReader) searcher.getIndexReader()).isCurrent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<EntityInfo> searchAllField(String searchTerm, int maxResults) {
        Set<EntityInfo> set = new LinkedHashSet<>();

        Query query = createQueryForAllFieldSearch(searchTerm);
        try {
            TopDocs topDocs = searcher.search(query, maxResults);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                String strEntityId = doc.getField(FLD_ID).stringValue();
                String text = storeContentInIndex ? doc.getField(FLD_ALL).stringValue() : null;
                EntityInfo entityInfo = new EntityInfo(entityName, parseIdFromString(strEntityId, entityName), text, false);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList(set);
    }

    public List<EntityInfo> searchAllField(String searchTerm, Collection<String> entityNames) {
        Set<EntityInfo> set = new LinkedHashSet<>();

        Query query = createQueryForAllFieldSearch(searchTerm, entityNames);

        try {
            AllDocsCollector collector = new AllDocsCollector();
            searcher.search(query, collector);
            for (Integer docId : collector.getDocIds()) {
                Document doc = searcher.doc(docId);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                String strEntityId = doc.getField(FLD_ID).stringValue();
                String text = storeContentInIndex ? doc.getField(FLD_ALL).stringValue() : null;
                EntityInfo entityInfo = new EntityInfo(entityName, parseIdFromString(strEntityId, entityName), text, false);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(set);
    }


    protected Query createQueryForAllFieldSearch(String searchTerm) {
        Query query;

        ValueFormatter valueFormatter = new ValueFormatter();

        if (searchTerm.startsWith("\"") && searchTerm.endsWith("\"")) {
            searchTerm = searchTerm.substring(1, searchTerm.length() - 1);
            query = new PhraseQuery();
            FTS.Tokenizer tokenizer = new FTS.Tokenizer(searchTerm);
            while (tokenizer.hasMoreTokens()) {
                Term term = new Term(FLD_ALL, tokenizer.nextToken());
                ((PhraseQuery) query).add(term);
            }
        } else {
            String[] strings = searchTerm.split("\\s");
            if (strings.length == 1) {
                query = createQuery(searchTerm, valueFormatter);
            } else {
                query = new BooleanQuery();
                for (String string : strings) {
                    if (StringUtils.isNotEmpty(string)) {
                        Query q = createQuery(string, valueFormatter);
                        ((BooleanQuery) query).add(q, BooleanClause.Occur.SHOULD);
                    }
                }
            }
        }
        return query;
    }

    protected Query createQueryForAllFieldSearch(String searchTerm, Collection<String> entityNames) {
        BooleanQuery query = new BooleanQuery();
        Query queryForAllFieldSearch = createQueryForAllFieldSearch(searchTerm);

        BooleanQuery entityNamesQuery = new BooleanQuery();
        for (String entityName : entityNames) {
            Term term = new Term(FLD_ENTITY, entityName);
            entityNamesQuery.add(new TermQuery(term), BooleanClause.Occur.SHOULD);
        }

        query.add(queryForAllFieldSearch, BooleanClause.Occur.MUST);
        query.add(entityNamesQuery, BooleanClause.Occur.MUST);
        return query;
    }

    protected Query createQueryForLinksFieldSearch(Object id, List<String> entityNames) {
        BooleanQuery query = new BooleanQuery();
        TermQuery idQuery = new TermQuery(new Term(FLD_LINKS, id.toString()));
        BooleanQuery entityNamesQuery = new BooleanQuery();
        for (String entityName : entityNames) {
            Term term = new Term(FLD_ENTITY, entityName);
            entityNamesQuery.add(new TermQuery(term), BooleanClause.Occur.SHOULD);
        }
        query.add(idQuery, BooleanClause.Occur.MUST);
        query.add(entityNamesQuery, BooleanClause.Occur.MUST);
        return query;
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
            BooleanQuery booleanQuery = new BooleanQuery();
            Term term = new Term(FLD_ALL, s);
            MorphologyNormalizer morphologyNormalizer = new MorphologyNormalizer();
            Term morphologyTerm = new Term(FLD_MORPHOLOGY_ALL, morphologyNormalizer.getAnyNormalForm(s));
            booleanQuery.add(new PrefixQuery(term), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(morphologyTerm), BooleanClause.Occur.SHOULD);
            query = booleanQuery;
        }
        return query;
    }

    protected Object parseIdFromString(String strId, String entityName) {
        Metadata metadata = AppBeans.get(Metadata.class);
        MetaClass metaClass = metadata.getSession().getClassNN(entityName);
        MetaProperty primaryKey = metadata.getTools().getPrimaryKeyProperty(metaClass);
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

    public List<EntityInfo> searchLinksField(Object id, int maxResults) {
        Set<EntityInfo> set = new LinkedHashSet<>();
        Term term = new Term(FLD_LINKS, id.toString());
        Query termQuery = new TermQuery(term);
        try {
            TopDocs topDocs = searcher.search(termQuery, maxResults);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                String strEntityId = doc.getField(FLD_ID).stringValue();
                EntityInfo entityInfo = new EntityInfo(entityName, parseIdFromString(strEntityId, entityName), null, true);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList(set);
    }

    public List<EntityInfo> searchLinksField(Object id, List<String> entityNames) {
        Set<EntityInfo> set = new LinkedHashSet<>();
        Query query = createQueryForLinksFieldSearch(id, entityNames);
        try {
            AllDocsCollector collector = new AllDocsCollector();
            searcher.search(query, collector);
            for (Integer docId : collector.getDocIds()) {
                Document doc = searcher.doc(docId);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                String strEntityId = doc.getField(FLD_ID).stringValue();
                String text = storeContentInIndex ? doc.getField(FLD_ALL).stringValue() : null;
                EntityInfo entityInfo = new EntityInfo(entityName, parseIdFromString(strEntityId, entityName), text, true);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(set);
    }

}
