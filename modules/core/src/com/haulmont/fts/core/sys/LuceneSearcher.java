/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.core.sys;

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
                UUID entityId = UUID.fromString(doc.getField(FLD_ID).stringValue());
                String text = storeContentInIndex ? doc.getField(FLD_ALL).stringValue() : null;
                EntityInfo entityInfo = new EntityInfo(entityName, entityId, text, false);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList(set);
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

    public List<EntityInfo> searchLinksField(UUID id, int maxResults) {
        Set<EntityInfo> set = new LinkedHashSet<>();
        Term term = new Term(FLD_LINKS, id.toString());
        Query termQuery = new TermQuery(term);
        try {
            TopDocs topDocs = searcher.search(termQuery, maxResults);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String entityName = doc.getField(FLD_ENTITY).stringValue();
                UUID entityId = UUID.fromString(doc.getField(FLD_ID).stringValue());
                EntityInfo entityInfo = new EntityInfo(entityName, entityId, null, true);
                set.add(entityInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList(set);
    }

}
