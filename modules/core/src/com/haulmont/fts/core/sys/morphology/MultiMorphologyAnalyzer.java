/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.fts.core.sys.morphology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.analyzer.MorphologyFilter;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * @author tsarevskiy
 * @version $Id$
 */
public class MultiMorphologyAnalyzer extends Analyzer {

    private static Log log = LogFactory.getLog(MultiMorphologyAnalyzer.class);

    protected List<LuceneMorphology> morphologies;

    protected Analyzer analyzer;

    public MultiMorphologyAnalyzer(List<LuceneMorphology> morphologies,
                                   Analyzer analyzer) {
        this.morphologies = morphologies;
        this.analyzer = analyzer;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        try {
            if (analyzer.tokenStream(fieldName, reader) instanceof Tokenizer) {
                Tokenizer analyzerTokenizer = (Tokenizer) analyzer.tokenStream(fieldName, reader);
                return new TokenStreamComponents(analyzerTokenizer,
                        addMorphologyFilter(analyzer.tokenStream(fieldName, reader)));
            }
        } catch (IOException e) {
            log.error("Error", e);
        }
        return null;
    }

    protected TokenStream addMorphologyFilter(TokenStream token) {
        for (LuceneMorphology morphology : morphologies) {
            token = new MorphologyFilter(token, morphology);
        }
        return token;
    }
}
