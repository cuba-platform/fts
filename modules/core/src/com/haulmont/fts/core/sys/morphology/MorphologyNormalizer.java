/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.fts.core.sys.morphology;

import com.haulmont.fts.global.Normalizer;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author tsarevskiy
 * @version $Id$
 */
public class MorphologyNormalizer implements Normalizer {

    protected static List<LuceneMorphology> morphologies = new ArrayList<LuceneMorphology>();

    static {
        try {
            morphologies.add(new RussianLuceneMorphology());
            morphologies.add(new EnglishLuceneMorphology());
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static List<LuceneMorphology> getAvailableMorphologies() {
        return morphologies;
    }

    public MorphologyNormalizer() {
    }

    public String getAnyNormalForm(String word) {
        for (LuceneMorphology morphology : morphologies) {
            if (morphology.checkString(word)) {
                return morphology.getNormalForms(word).iterator().next();
            }
        }
        return word;
    }

    public List<String> getAllNormalForms(String word) {
        for (LuceneMorphology morphology : morphologies) {
            if (morphology.checkString(word)) {
                return morphology.getNormalForms(word);
            }
        }
        return Collections.singletonList(word);
    }

}
