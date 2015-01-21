/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.fts.core.sys;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This Collector implementation is used for searching and collecting all results
 * of lucene search. Internal lucene collectors restrict number of documents in a result by
 * some number.
 *
 * @author gorbunkov
 * @version $Id$
 */
public class AllDocsCollector extends Collector {

    private int docBase;

    private List<Integer> docIds = new ArrayList<>();

    @Override
    public void setScorer(Scorer scorer) throws IOException {
        //do nothing
    }

    @Override
    public void collect(int doc) throws IOException {
        docIds.add(docBase + doc);
    }

    @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
        this.docBase = context.docBase;
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return true;
    }

    public List<Integer> getDocIds() {
        return docIds;
    }
}
