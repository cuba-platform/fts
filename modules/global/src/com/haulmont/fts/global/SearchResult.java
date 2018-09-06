/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
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