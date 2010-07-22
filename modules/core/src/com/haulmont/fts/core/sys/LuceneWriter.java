package com.haulmont.fts.core.sys;

import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import java.io.IOException;

public class LuceneWriter {

    protected Directory directory;
    protected PerFieldAnalyzerWrapper analyzer;
    protected IndexWriter writer;

    protected static final String FLD_ENTITY = "entity";
    protected static final String FLD_ID = "id";
    protected static final String FLD_ALL = "all";
    protected static final String FLD_LINKS = "links";

    public LuceneWriter(Directory directory) {
        this.directory = directory;
        analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_30));
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