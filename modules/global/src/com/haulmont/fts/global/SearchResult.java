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
package com.haulmont.fts.global;

import java.io.Serializable;
import java.util.*;

/**
 * Contains information about searched entities and searched page {@link QueryKey}
 */
public class SearchResult implements Serializable {
    private static final long serialVersionUID = -7860852850200335906L;

    protected String searchTerm;

    protected Map<String, Set<SearchResultEntry>> results = new HashMap<>();
    protected Map<Object, HitInfo> hits = new HashMap<>();
    protected QueryKey queryKey;

    public SearchResult(String searchTerm) {
        this.searchTerm = searchTerm;
        this.queryKey = new QueryKey();
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public Set<SearchResultEntry> getEntries(String entityName) {
        Set<SearchResultEntry> entries = results.get(entityName);
        return entries == null ? Collections.emptySet() : Collections.unmodifiableSet(entries);
    }

    public Set<SearchResultEntry> getAllEntries() {
        Set<SearchResultEntry> allEntries = new LinkedHashSet<>();
        for (Set<SearchResultEntry> entries: results.values()) {
            allEntries.addAll(entries);
        }
        return allEntries;
    }

    public void addEntry(SearchResultEntry entry) {
        Set<SearchResultEntry> set = results.computeIfAbsent(entry.getEntityName(), k -> new LinkedHashSet<>());
        set.add(entry);
    }

    public Collection<String> getEntityNames() {
        return results.keySet();
    }

    public int getCount() {
        return results.values().stream().mapToInt(Set::size).sum();
    }

    public int getIdsCount() {
        return queryKey.getIds().size();
    }

    public void addHit(Object id, String entityName,
                       String text, Normalizer normalizer) {
        EntityKey entityKey = new EntityKey(id, entityName);
        HitInfo hi = hits.computeIfAbsent(entityKey, key -> new HitInfo());
        hi.init(searchTerm, text, null, normalizer);
    }

    public void addLinkedHit(Object id, String entityName,
                             String text, String linkedEntityName,
                             Normalizer normalizer) {
        EntityKey entityKey = new EntityKey(id, entityName);
        HitInfo hi = hits.computeIfAbsent(entityKey, key -> new HitInfo());
        hi.init(searchTerm, text, linkedEntityName, normalizer);
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public HitInfo getHitInfo(Object id, String entityName) {
        return hits.get(new EntityKey(id, entityName));
    }

    public QueryKey getQueryKey() {
        return queryKey;
    }
}