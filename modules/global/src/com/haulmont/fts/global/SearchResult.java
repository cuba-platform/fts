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

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

/**
 * Contains information about searched entities and searched page {@link QueryKey}
 */
public class SearchResult implements Serializable {
    private static final long serialVersionUID = -7860852850200335906L;

    protected String searchTerm;
    protected QueryKey queryKey;
    protected Map<EntityInfo, SearchResultEntry> entriesByEntityInfo = new HashMap<>();
    protected Map<String, Set<SearchResultEntry>> entriesByEntityName = new HashMap<>();

    public SearchResult(String searchTerm) {
        this.searchTerm = searchTerm;
        this.queryKey = new QueryKey();
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void addEntry(SearchResultEntry searchResultEntry) {
        entriesByEntityInfo.put(searchResultEntry.getEntityInfo(), searchResultEntry);
        Set<SearchResultEntry> entriesForEntityName = entriesByEntityName.computeIfAbsent(searchResultEntry.getEntityInfo().getEntityName(),
                s -> new LinkedHashSet<>());
        entriesForEntityName.add(searchResultEntry);
    }

    @Nullable
    public SearchResultEntry getEntryByEntityInfo(EntityInfo entityInfo) {
        return entriesByEntityInfo.get(entityInfo);
    }

    public Set<SearchResultEntry> getEntriesByEntityName(String entityName) {
        Set<SearchResultEntry> entries = entriesByEntityName.get(entityName);
        return entries == null ? Collections.emptySet() : Collections.unmodifiableSet(entries);
    }

    public Set<SearchResultEntry> getAllEntries() {
        return new HashSet<>(entriesByEntityInfo.values());
    }

    public Collection<String> getEntityNames() {
        return entriesByEntityName.keySet();
    }

    public int getCount() {
        return entriesByEntityInfo.values().size();
    }

    public int getIdsCount() {
        return queryKey.getEntityInfos().size();
    }

    public boolean isEmpty() {
        return entriesByEntityInfo.isEmpty();
    }

    public QueryKey getQueryKey() {
        return queryKey;
    }
}