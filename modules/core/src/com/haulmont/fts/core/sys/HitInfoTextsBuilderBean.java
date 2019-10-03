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

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.fts.core.sys.morphology.MorphologyNormalizer;
import com.haulmont.fts.global.FTS;
import com.haulmont.fts.global.FtsConfig;
import com.haulmont.fts.global.Normalizer;
import com.haulmont.fts.global.ValueFormatter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(HitInfoTextsBuilder.NAME)
public class HitInfoTextsBuilderBean implements HitInfoTextsBuilder {

    @Override
    public Map<String, String> buildHighlightedHitTexts(String fieldAllContent, String searchTerm) {
        Normalizer normalizer = createNormalizer();
        Map<String, String> hits = new HashMap<>();
        boolean phraseSearch = searchTerm.startsWith("\"") && searchTerm.endsWith("\"");
        boolean likeSearch = searchTerm.startsWith("*");

        ValueFormatter valueFormatter = new ValueFormatter();
        StringBuilder searchTermBuilder = new StringBuilder();
        String[] strings = searchTerm.split("\\s");
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            String s = valueFormatter.guessTypeAndFormat(string);
            if (s != null) {
                searchTermBuilder.append(s);
            }
            if (i < strings.length - 1)
                searchTermBuilder.append(" ");
        }
        searchTerm = searchTermBuilder.toString();
        String lowerSearchTerm = searchTerm.toLowerCase();

        Map<String, String> fieldsMap = new HashMap<String, String>();
        List<String> terms = new ArrayList<>();

        if (!phraseSearch) {
            FTS.Tokenizer termTokenizer = new FTS.Tokenizer(lowerSearchTerm);
            while (termTokenizer.hasMoreTokens()) {
                String term = termTokenizer.nextToken();
                String normTerm = null;
                if (normalizer != null) {
                    normTerm = normalizer.getAnyNormalForm(term);
                }
                if (StringUtils.isBlank(term))
                    continue;
                terms.add(term);

                String[] fields = fieldAllContent.split(FTS.FIELD_START_RE);
                for (String field : fields) {
                    if (StringUtils.isBlank(field))
                        continue;

                    int nameEnd = field.indexOf(" ");
                    if (nameEnd == -1)
                        continue;

                    String fieldName = field.substring(0, nameEnd).replace(FTS.FIELD_SEP, ".");
                    String fieldText = field.substring(nameEnd);

                    FTS.Tokenizer tokenizer = new FTS.Tokenizer(fieldText);
                    outerWhile:
                    while (tokenizer.hasMoreTokens()) {
                        String word = tokenizer.nextToken().toLowerCase();
                        if (!likeSearch && normalizer != null) {
                            List<String> normalForms = normalizer.getAllNormalForms(word);
                            for (String normalForm : normalForms) {
                                if (normalForm.equals(normTerm)) {
                                    fieldsMap.put(fieldName, fieldText);
                                    break outerWhile;
                                }
                            }
                        }
                        if (likeSearch ? word.contains(term) : word.startsWith(term)) {
                            fieldsMap.put(fieldName, fieldText);
                            break;
                        }
                    }
                }
            }
        } else {
            FTS.Tokenizer termTokenizer = new FTS.Tokenizer(lowerSearchTerm);
            while (termTokenizer.hasMoreTokens()) {
                String term = termTokenizer.nextToken();
                terms.add(term);
            }
            String[] fields = fieldAllContent.split(FTS.FIELD_START_RE);
            fieldsFor:
            for (String field : fields) {
                if (StringUtils.isBlank(field))
                    continue;

                int nameEnd = field.indexOf(" ");
                if (nameEnd == -1)
                    continue;

                String fieldName = field.substring(0, nameEnd).replace(FTS.FIELD_SEP, ".");
                String fieldText = field.substring(nameEnd);

                FTS.Tokenizer tokenizer = new FTS.Tokenizer(fieldText);
                while (tokenizer.hasMoreTokens()) {
                    String word = tokenizer.nextToken();
                    int start, end = fieldText.length();
                    if (word.equalsIgnoreCase(terms.get(0))) {
                        start = Math.max(tokenizer.getTokenStart() - FTS.HIT_CONTEXT_PAD, 0);
                        while (start > 0 && FTS.isTokenChar(fieldText.charAt(start)))
                            start--;
                        for (int i = 1; i < terms.size(); i++) {
                            String term = terms.get(i);
                            if (!tokenizer.hasMoreTokens()) {
                                end = -1;
                                break;
                            }
                            String w = tokenizer.nextToken();
                            if (!w.equalsIgnoreCase(term)) {
                                end = -1;
                                break;
                            }
                            end = Math.min(tokenizer.getTokenEnd() + FTS.HIT_CONTEXT_PAD, fieldText.length());
                        }
                        if (end != -1) {
                            fieldsMap.put(fieldName, fieldText);
                            continue fieldsFor;
                        }
                    }
                }
            }
        }

        int maxNumberOfSearchTermsInHitInfo = AppBeans.get(Configuration.class)
                .getConfig(FtsConfig.class)
                .getMaxNumberOfSearchTermsInHitInfo();

        for (Map.Entry<String, String> entry : fieldsMap.entrySet()) {
            String fieldName = entry.getKey();
            String fieldText = entry.getValue();

            if (phraseSearch) {
                hits.put(fieldName, makeFieldPhraseText(terms, fieldText, maxNumberOfSearchTermsInHitInfo));
            } else {
                hits.put(fieldName, makeFieldText(terms, fieldText, likeSearch, normalizer, maxNumberOfSearchTermsInHitInfo));
            }
        }

        return hits;
    }


    protected String makeFieldText(List<String> terms, String fieldText, boolean likeSearch,
                               Normalizer normalizer, int maxNumberOfSearchTerms) {
        int numberOfSearchTermsFound = 0;
        StringBuilder sb = new StringBuilder();
        FTS.Tokenizer tokenizer = new FTS.Tokenizer(fieldText);
        outerLoop:
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken().toLowerCase();
            List<String> normalForms = null;
            if (normalizer != null) {
                normalForms = normalizer.getAllNormalForms(word);
            }
            for (String term : terms) {
                String normalTerm = null;
                if (normalizer != null) {
                    normalTerm = normalizer.getAnyNormalForm(term);
                }
                if (likeSearch ? word.contains(term) : word.startsWith(term)) {
                    makePartTextField(tokenizer, fieldText, sb);
                    if (++numberOfSearchTermsFound >= maxNumberOfSearchTerms)
                        break outerLoop;
                } else {
                    if (!likeSearch && normalizer != null) {
                        for (String form : normalForms) {
                            if (form.equals(normalTerm)) {
                                makePartTextField(tokenizer, fieldText, sb);
                                if (++numberOfSearchTermsFound >= maxNumberOfSearchTerms)
                                    break outerLoop;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    protected void makePartTextField(FTS.Tokenizer tokenizer, String fieldText, StringBuilder sb) {
        int start = Math.max(tokenizer.getTokenStart() - FTS.HIT_CONTEXT_PAD, 0);
        while (start > 0 && FTS.isTokenChar(fieldText.charAt(start)))
            start--;

        int end = Math.min(tokenizer.getTokenEnd() + FTS.HIT_CONTEXT_PAD, fieldText.length());
        while (end < fieldText.length() && FTS.isTokenChar(fieldText.charAt(end)))
            end++;

        if (start > 0 && !sb.toString().endsWith("..."))
            sb.append("...");
        sb.append(fieldText.substring(start, tokenizer.getTokenStart()));
        sb.append("<b>");
        sb.append(fieldText.substring(tokenizer.getTokenStart(), tokenizer.getTokenEnd()));
        sb.append("</b>");
        if (end <= fieldText.length()) {
            sb.append(fieldText.substring(tokenizer.getTokenEnd(), end));
            if (end < fieldText.length()) {
                sb.append("...");
            }
        }
    }

    protected String makeFieldPhraseText(List<String> terms, String fieldText, int maxNumberOfSearchTerms) {
        int numberOfSearchTermsFound = 0;
        StringBuilder sb = new StringBuilder();
        FTS.Tokenizer tokenizer = new FTS.Tokenizer(fieldText);
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            int start, end = fieldText.length();
            if (word.equalsIgnoreCase(terms.get(0))) {
                int startFirstWord = tokenizer.getTokenStart();
                start = Math.max(tokenizer.getTokenStart() - FTS.HIT_CONTEXT_PAD, 0);
                while (start > 0 && FTS.isTokenChar(fieldText.charAt(start)))
                    start--;
                if (terms.size() == 1) {
                    end = Math.min(tokenizer.getTokenEnd() + FTS.HIT_CONTEXT_PAD, fieldText.length());
                    while (end < fieldText.length() && FTS.isTokenChar(fieldText.charAt(end)))
                        end++;
                } else {
                    for (int i = 1; i < terms.size(); i++) {
                        String term = terms.get(i);
                        if (!tokenizer.hasMoreTokens()) {
                            end = -1;
                            break;
                        }
                        String w = tokenizer.nextToken();
                        if (!w.equalsIgnoreCase(term)) {
                            end = -1;
                            break;
                        }
                        end = Math.min(tokenizer.getTokenEnd() + FTS.HIT_CONTEXT_PAD, fieldText.length());
                        while (end < fieldText.length() && FTS.isTokenChar(fieldText.charAt(end)))
                            end++;
                    }
                }
                if (end == -1) {
                    continue;
                }
                String phrase =
                        highlightPhraseTerms(fieldText.substring(start, end), terms, startFirstWord - start);

                if (start > 0 && !sb.toString().endsWith("..."))
                    sb.append("...");
                sb.append(phrase);
                if (end < fieldText.length())
                    sb.append("...");
                if (++numberOfSearchTermsFound >= maxNumberOfSearchTerms)
                    break;

            }
        }
        return sb.toString().trim();
    }

    protected String highlightPhraseTerms(String text, List<String> terms, int startFirstWord) {
        String lowerText = text.toLowerCase();
        int savedCut = startFirstWord;
        lowerText = lowerText.substring(savedCut);
        int start = lowerText.indexOf(terms.get(0));
        int globalStart = start + savedCut;
        if (start == -1) {
            return text;
        }
        int cutIndex = start + terms.get(0).length();
        lowerText = lowerText.substring(cutIndex);
        savedCut += cutIndex;

        for (int i = 1; i < terms.size() - 1; i++) {
            int index = lowerText.indexOf(terms.get(i));
            if (index == -1) {
                return text;
            }
            cutIndex = index + terms.get(i).length();
            lowerText = lowerText.substring(cutIndex);
            savedCut += cutIndex;
        }
        int globalEnd;
        if (terms.size() == 1) {
            globalEnd = globalStart + terms.get(0).length();
        } else {
            int end = lowerText.indexOf(terms.get(terms.size() - 1)) + terms.get(terms.size() - 1).length();
            if (end == -1)
                return text;
            globalEnd = end + savedCut;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(text.substring(0, globalStart));
        sb.append("<b>");
        sb.append(text.substring(globalStart, globalEnd));
        sb.append("</b>");
        sb.append(text.substring(globalEnd));
        return sb.toString();
    }

    protected Normalizer createNormalizer() {
        return new MorphologyNormalizer();
    }
}
