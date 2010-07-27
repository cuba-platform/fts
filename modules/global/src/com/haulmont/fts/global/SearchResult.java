/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.07.2010 15:31:09
 *
 * $Id$
 */
package com.haulmont.fts.global;

import java.io.Serializable;
import java.util.*;

public class SearchResult implements Serializable {

    private static final long serialVersionUID = -7860852850200335906L;

    public static class Entry implements Serializable {

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
    }

    private Map<String, Set<Entry>> results = new HashMap<String, Set<Entry>>();
    private Map<String, Set<UUID>> ids = new HashMap<String, Set<UUID>>();


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
}
