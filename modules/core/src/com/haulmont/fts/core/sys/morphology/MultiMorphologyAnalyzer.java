package com.haulmont.fts.core.sys.morphology;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
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

    protected List<LuceneMorphology> morphologies;

    protected Analyzer analyzer;

    public MultiMorphologyAnalyzer(List<LuceneMorphology> morphologies,
                                   Analyzer analyzer) {
        this.morphologies = morphologies;
        this.analyzer = analyzer;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return addMorphologyFilter(analyzer.tokenStream(fieldName, reader));
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        return addMorphologyFilter(analyzer.reusableTokenStream(fieldName, reader));
    }

    protected TokenStream addMorphologyFilter(TokenStream token) {
        for (LuceneMorphology morphology : morphologies) {
            token = new MorphologyFilter(token, morphology);
        }
        return token;
    }

}
