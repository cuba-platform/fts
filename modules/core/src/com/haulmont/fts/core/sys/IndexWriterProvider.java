/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
