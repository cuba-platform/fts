/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.global;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

public class SearchResult implements Serializable {

    private static final long serialVersionUID = -7860852850200335906L;

    private String searchTerm;

    public SearchResult(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public static class Entry implements Serializable, Comparable<Entry> {

        private static final long serialVersionUID = -1033032285547581245L;

        private UUID id;
        private String caption;

        public Entry(UUID id, String caption) {
            this.caption = caption;
            this.id = id;
        }

        public UUID getId() {
            return id;
        }

        public String getCaption() {
            return caption;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (!id.equals(entry.id)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public int compareTo(Entry o) {
            String c1 = caption == null ? "" : caption;
            String c2 = o.caption == null ? "" : o.caption;
            return c1.compareTo(c2);
        }
    }

    public static class HitInfo implements Serializable {

        private static final long serialVersionUID = 1418133997886915727L;

        private Map<String, String> hits = new HashMap<String, String>();

        public void init(String searchTerm, String text, String entityName) {
            init(searchTerm, text, entityName, null);
        }

        public void init(String searchTerm, String text, String entityName, Normalizer normalizer) {
            boolean phraseSearch = searchTerm.startsWith("\"") && searchTerm.endsWith("\"");
            boolean likeSearch = searchTerm.startsWith("*");

            ValueFormatter valueFormatter = new ValueFormatter();
            StringBuilder searchTermBuilder = new StringBuilder();
            String[] strings = searchTerm.split("\\s");
            for (int i = 0; i < strings.length; i++) {
                String string = strings[i];
                String s = valueFormatter.guessTypeAndFormat(string);
                searchTermBuilder.append(s);
                if (i < strings.length - 1)
                    searchTermBuilder.append(" ");
            }
            searchTerm = searchTermBuilder.toString();
            
            Map<String, String> fieldsMap = new HashMap<String, String>();

            List<String> terms = new ArrayList();
            FTS.Tokenizer termTokenizer = new FTS.Tokenizer(searchTerm.toLowerCase());
            while (termTokenizer.hasMoreTokens()) {
                String term = termTokenizer.nextToken();
                String normTerm = null;
                if (normalizer != null) {
                    normTerm = normalizer.getAnyNormalForm(term);
                }
                if (StringUtils.isBlank(term))
                    continue;
                terms.add(term);

                String[] fields = text.split(FTS.FIELD_START_RE);
                for (String field : fields) {
                    if (StringUtils.isBlank(field))
                        continue;

                    int nameEnd = field.indexOf(" ");
                    if (nameEnd == -1)
                        continue;

                    String fieldName = field.substring(0, nameEnd).replace(FTS.FIELD_SEP, ".");
                    if (entityName != null)
                        fieldName = entityName + "." + fieldName;
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

            for (Map.Entry<String, String> entry : fieldsMap.entrySet()) {
                String fieldName = entry.getKey();
                String fieldText = entry.getValue();

                if (phraseSearch) {
                    makeFieldPhraseText(terms, fieldName, fieldText);
                } else {
                    makeFieldText(terms, fieldName, fieldText, likeSearch, normalizer);
                }
            }
        }

        private void makeFieldText(List<String> terms, String fieldName, String fieldText, boolean likeSearch,
                                   Normalizer normalizer) {
            StringBuilder sb = new StringBuilder();
            FTS.Tokenizer tokenizer = new FTS.Tokenizer(fieldText);
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
                    } else {
                        if (!likeSearch && normalizer != null) {
                            for (String form : normalForms) {
                                if (form.equals(normalTerm)) {
                                    makePartTextField(tokenizer, fieldText, sb);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            hits.put(fieldName, sb.toString());
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

        private void makeFieldPhraseText(List<String> terms, String fieldName, String fieldText) {
            StringBuilder sb = new StringBuilder();
            FTS.Tokenizer tokenizer = new FTS.Tokenizer(fieldText);
            while (tokenizer.hasMoreTokens()) {
                String word = tokenizer.nextToken();
                int start, end = fieldText.length();
                if (word.equalsIgnoreCase(terms.get(0))) {
                    start = Math.max(tokenizer.getTokenStart() - FTS.HIT_CONTEXT_PAD, 0);
                    while (start > 0 && FTS.isTokenChar(fieldText.charAt(start)))
                        start--;
                    for (int i = 1; i < terms.size() && tokenizer.hasMoreTokens(); i++) {
                        String term = terms.get(i);
                        String w = tokenizer.nextToken();
                        if (!w.equalsIgnoreCase(term)) {
                            end = -1;
                            break;
                        }
                        end = Math.min(tokenizer.getTokenEnd() + FTS.HIT_CONTEXT_PAD, fieldText.length());
                        while (end < fieldText.length() && FTS.isTokenChar(fieldText.charAt(end)))
                            end++;
                    }
                    if (end == -1) {
                        continue;
                    }
                    String phrase = highlightPhraseTerms(fieldText.substring(start, end), terms);

                    if (start > 0 && !sb.toString().endsWith("..."))
                        sb.append("...");
                    sb.append(phrase);
                    if (end < fieldText.length())
                        sb.append("...");
                }
            }
            hits.put(fieldName, sb.toString());
        }

        private String highlightPhraseTerms(String text, List<String> terms) {
            int start = text.toLowerCase().indexOf(terms.get(0));
            int end = text.toLowerCase().indexOf(terms.get(terms.size() - 1)) + terms.get(terms.size() - 1).length();
            if (start == -1 || end == -1)
                return text;

            StringBuilder sb = new StringBuilder();
            sb.append(text.substring(0, start));
            sb.append("<b>");
            sb.append(text.substring(start, end));
            sb.append("</b>");
            sb.append(text.substring(end));
            return sb.toString();
        }

        public Map<String, String> getHits() {
            return hits;
        }

        @Override
        public String toString() {
            return hits.toString();
        }
    }

    private Map<String, Set<Entry>> results = new HashMap<String, Set<Entry>>();
    private Map<String, Set<UUID>> ids = new HashMap<String, Set<UUID>>();
    private Map<UUID, HitInfo> hitInfos = new HashMap<UUID, HitInfo>();

    public List<String> getEntities() {
        return new ArrayList(results.keySet());
    }

    public List<Entry> getEntries(String entityName) {
        Set<Entry> entries = results.get(entityName);
        return entries == null ? new ArrayList() : new ArrayList(entries);
    }

    public int getEntriesCount(String entityName) {
        Set<Entry> entries = results.get(entityName);
        return entries == null ? 0 : entries.size();
    }

    public void addEntry(String entityName, Entry entry) {
        Set<Entry> set = results.get(entityName);
        if (set == null) {
            set = new LinkedHashSet<Entry>();
            results.put(entityName, set);
        }
        set.add(entry);
    }

    public void addId(String entityName, UUID id) {
        Set<UUID> set = ids.get(entityName);
        if (set == null) {
            set = new LinkedHashSet<UUID>();
            ids.put(entityName, set);
        }
        set.add(id);
    }

    public void addHit(UUID id, String text, String linkedEntityName) {
        addHit(id, text, linkedEntityName, null);
    }

    public void addHit(UUID id, String text, String linkedEntityName, Normalizer normalizer) {
        HitInfo hi = hitInfos.get(id);
        if (hi == null) {
            hi = new HitInfo();
            hitInfos.put(id, hi);
        }
        hi.init(searchTerm, text, linkedEntityName, normalizer);
    }

    public List<UUID> getIds(String entityName) {
        Set<UUID> set = ids.get(entityName);
        return set == null ? new ArrayList() : new ArrayList(set);
    }

    public void removeId(String entityName, UUID id) {
        Set<UUID> set = ids.get(entityName);
        if (set != null)
            set.remove(id);
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public boolean hasEntry(String entityName, UUID id) {
        Set<Entry> entries = results.get(entityName);
        if (entries == null)
            return false;
        else {
            for (Entry entry : entries) {
                if (entry.getId().equals(id))
                    return true;
            }
            return false;
        }
    }

    public HitInfo getHitInfo(UUID id) {
        return hitInfos.get(id);
    }
}
