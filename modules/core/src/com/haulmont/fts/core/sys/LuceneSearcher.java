/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.07.2010 15:48:35
 *
 * $Id$
 */
package com.haulmont.fts.core.sys;

import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.ValueFormatter;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
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
            searcher = new IndexSearcher(directory, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isCurrent() {
        try {
            return searcher.getIndexReader().isCurrent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<EntityInfo> searchAllField(String searchTerm, int maxResults) {
        Set<EntityInfo> set = new LinkedHashSet<EntityInfo>();

        ValueFormatter valueFormatter = new ValueFormatter();

        Query query;

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
                    Query q = createQuery(string, valueFormatter);
                    ((BooleanQuery) query).add(q, BooleanClause.Occur.SHOULD);
                }
            }
        }
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

    private Query createQuery(String searchStr, ValueFormatter valueFormatter) {
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
        Set<EntityInfo> set = new LinkedHashSet<EntityInfo>();
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
