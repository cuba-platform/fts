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

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This Collector implementation is used for searching and collecting all results
 * of lucene search. Internal lucene collectors restrict number of documents in a result by
 * some number.
 *
 */
public class AllDocsCollector extends SimpleCollector {

    private int docBase;

    private List<Integer> docIds = new ArrayList<>();

    @Override
    public void collect(int doc) throws IOException {
        docIds.add(docBase + doc);
    }

    @Override
    public void doSetNextReader(LeafReaderContext context) throws IOException {
        this.docBase = context.docBase;
    }

    public List<Integer> getDocIds() {
        return docIds;
    }

    @Override
    public boolean needsScores() {
        return false;
    }
}
