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

package com.haulmont.fts.core.sys.morphology;

import com.haulmont.fts.core.sys.EntityAttributeTokenizer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.analyzer.MorphologyFilter;

import java.util.List;

public class MultiMorphologyAnalyzer extends Analyzer {
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