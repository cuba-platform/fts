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

    public List<String> getEntities() {
        return new ArrayList(results.keySet());
    }

    public List<Entry> getEntries(String entityName) {
        return new ArrayList(results.get(entityName));
    }

    public void addEntry(String entityName, Entry entry) {
        Set<Entry> set = results.get(entityName);
        if (set == null) {
            set = new LinkedHashSet<Entry>();
            results.put(entityName, set);
        }
        set.add(entry);
    }
    
    public boolean isEmpty() {
        return results.isEmpty();
    }
}
