package com.haulmont.fts.core.sys;

import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.core.sys.morphology.MultiMorphologyAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.List;

public class LuceneWriter extends Lucene {

    protected PerFieldAnalyzerWrapper analyzer;
    protected IndexWriter writer;

    public LuceneWriter(Directory directory) {
        super(directory);

        List<LuceneMorphology> morphologies =
                MorphologyNormalizer.getAvailableMorphologies();
        analyzer = new PerFieldAnalyzerWrapper(new EntityAttributeAnalyzer());
        analyzer.addAnalyzer(FLD_LINKS, new WhitespaceAnalyzer());

        analyzer.addAnalyzer(FLD_MORPHOLOGY_ALL, new MultiMorphologyAnalyzer(morphologies,
                new EntityAttributeAnalyzer()));
        try {
            writer = new IndexWriter(directory, analyzer, new KeepOnlyLastCommitDeletionPolicy(), IndexWriter.MaxFieldLength.UNLIMITED);
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
            writer.optimize();
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

    public static void deleteIndexForEntity(Directory directory, String entityName) {
        try {
            IndexReader indexReader = IndexReader.open(directory, false);
            indexReader.deleteDocuments(new Term(FLD_ENTITY, entityName));
            indexReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}