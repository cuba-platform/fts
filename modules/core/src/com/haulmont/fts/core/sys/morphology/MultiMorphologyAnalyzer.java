/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.fts.core.sys.morphology;

import com.haulmont.fts.core.sys.EntityAttributeTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.analyzer.MorphologyFilter;
import java.util.List;

/**
 * @author tsarevskiy
 * @version $Id$
 */
public class MultiMorphologyAnalyzer extends Analyzer {

    private static Log log = LogFactory.getLog(MultiMorphologyAnalyzer.class);

    protected List<LuceneMorphology> morphologies;

    public MultiMorphologyAnalyzer(List<LuceneMorphology> morphologies) {
        this.morphologies = morphologies;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        EntityAttributeTokenizer tokenizer = new EntityAttributeTokenizer();
        return new TokenStreamComponents(tokenizer, addMorphologyFilter(tokenizer));
    }

    protected TokenStream addMorphologyFilter(TokenStream token) {
        for (LuceneMorphology morphology : morphologies) {
            token = new MorphologyFilter(token, morphology);
        }
        return token;
    }
}
