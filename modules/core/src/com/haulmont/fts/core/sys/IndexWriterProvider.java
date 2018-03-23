/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.core.sys.morphology.MultiMorphologyAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.search.SearcherManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.haulmont.fts.core.sys.LuceneConstants.*;

/**
 * Class is used for getting Lucene {@link IndexWriter} instance. After the required index modifications are done
 * (lucene documents are created, deleted, etc.), the {@link IndexWriter#commit()} method must be invoked.
 * <p>
 * After the {@link IndexWriter} is commited, the {@link SearcherManager#maybeRefresh()} should be invoked in order to
 * make the {@link org.apache.lucene.search.IndexSearcher} see the latest changes. The {@link SearcherManager} should be
 * obtained by the {@link IndexSearcherProvider}.
 * <p>
 * The application uses a single instance of the {@link IndexWriter}
 */
public interface IndexWriterProvider {

    String NAME = "fts_IndexWriterProvider";

    /**
     * Method returns an instance of the Lucene {@link IndexWriter}.
     */
    IndexWriter getIndexWriter();
}
