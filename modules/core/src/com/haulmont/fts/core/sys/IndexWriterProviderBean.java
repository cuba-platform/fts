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

import static com.haulmont.fts.core.sys.LuceneConstants.FLD_LINKS;
import static com.haulmont.fts.core.sys.LuceneConstants.FLD_MORPHOLOGY_ALL;

@Component(IndexWriterProvider.NAME)
public class IndexWriterProviderBean implements IndexWriterProvider {

    @Inject
    protected DirectoryProvider directoryProvider;

    protected volatile IndexWriter indexWriter;

    /**
     * Method returns an instance of the Lucene {@link IndexWriter}.
     */
    @Override
    public IndexWriter getIndexWriter() {
        if (indexWriter == null) {
            synchronized (this) {
                if (indexWriter == null) {
                    indexWriter = createWriter();
                }
            }
        }
        return indexWriter;
    }

    protected IndexWriter createWriter() {
        Analyzer analyzer = createAnalyzer();
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setIndexDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
            return new IndexWriter(directoryProvider.getDirectory(), config);
        } catch (IOException e) {
            throw new RuntimeException("Error on IndexWriter creation", e);
        }
    }

    protected Analyzer createAnalyzer() {
        List<LuceneMorphology> morphologies = MorphologyNormalizer.getAvailableMorphologies();
        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put(FLD_LINKS, new WhitespaceAnalyzer());
        analyzerPerField.put(FLD_MORPHOLOGY_ALL, new MultiMorphologyAnalyzer(morphologies));
        return new PerFieldAnalyzerWrapper(new EntityAttributeAnalyzer(), analyzerPerField);
    }
}
