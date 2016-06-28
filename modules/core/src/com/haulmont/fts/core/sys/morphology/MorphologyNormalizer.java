/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys.morphology;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.fts.global.Normalizer;
import org.apache.lucene.morphology.LuceneMorphology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MorphologyNormalizer implements Normalizer {

    protected static List<LuceneMorphology> morphologies = new ArrayList<>();

    static {
        try {
            Class<?> morphClass = ReflectionHelper.loadClass("org.apache.lucene.morphology.english.EnglishLuceneMorphology");
            morphologies.add((LuceneMorphology) morphClass.newInstance());
        } catch (ClassNotFoundException ignored) {
            // the dependency could be excluded
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error initializing FTS English morphology", e);
        }

        try {
            Class<?> morphClass = ReflectionHelper.loadClass("org.apache.lucene.morphology.russian.RussianLuceneMorphology");
            morphologies.add((LuceneMorphology) morphClass.newInstance());
        } catch (ClassNotFoundException ignored) {
            // the dependency could be excluded
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error initializing FTS Russian morphology", e);
        }
    }

    public static List<LuceneMorphology> getAvailableMorphologies() {
        return morphologies;
    }

    public MorphologyNormalizer() {
    }

    @Override
    public String getAnyNormalForm(String word) {
        for (LuceneMorphology morphology : morphologies) {
            if (morphology.checkString(word)) {
                return morphology.getNormalForms(word).iterator().next();
            }
        }
        return word;
    }

    @Override
    public List<String> getAllNormalForms(String word) {
        for (LuceneMorphology morphology : morphologies) {
            if (morphology.checkString(word)) {
                return morphology.getNormalForms(word);
            }
        }
        return Collections.singletonList(word);
    }
}