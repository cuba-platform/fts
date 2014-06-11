/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.fts.core.sys;

import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.core.sys.morphology.MultiMorphologyAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuceneWriter extends Lucene {

    protected IndexWriter writer;

    public LuceneWriter(Directory directory) {
        super(directory);
        writer = createWriter(directory);
    }

    public static IndexWriter createWriter(Directory directory) {
        List<LuceneMorphology> morphologies = MorphologyNormalizer.getAvailableMorphologies();

        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put(FLD_LINKS, new WhitespaceAnalyzer(Version.LUCENE_44));
        analyzerPerField.put(FLD_MORPHOLOGY_ALL, new MultiMorphologyAnalyzer(morphologies,
                new EntityAttributeAnalyzer()));
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new EntityAttributeAnalyzer(), analyzerPerField);
        try {
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_44, analyzer);
            config.setIndexDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
            return new IndexWriter(directory, config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void optimize() {
        try {
            writer.forceMerge(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAll() {
        try {
            writer.deleteAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteIndexForEntity(String entityName) {
        try {
            writer.deleteDocuments(new Term(FLD_ENTITY, entityName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}