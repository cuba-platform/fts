/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
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
