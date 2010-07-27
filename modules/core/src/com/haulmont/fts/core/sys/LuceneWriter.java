package com.haulmont.fts.core.sys;

import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;

import java.io.IOException;

public class LuceneWriter extends Lucene {

    protected PerFieldAnalyzerWrapper analyzer;
    protected IndexWriter writer;

    public LuceneWriter(Directory directory) {
        super(directory);

        analyzer = new PerFieldAnalyzerWrapper(new EntityAttributeAnalyzer());
        analyzer.addAnalyzer(FLD_LINKS, new WhitespaceAnalyzer());

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