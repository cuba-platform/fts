/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;

import java.util.*;

public class EntityDescr {

    private MetaClass metaClass;

    private Map<String, Boolean> properties = new HashMap<String, Boolean>();

    private String searchableIfScript;

    private String searchablesScript;

    private boolean show;

    public EntityDescr(MetaClass metaClass, String searchableIfScript, String searchablesScript, boolean show) {
        this.metaClass = metaClass;
        this.searchableIfScript = searchableIfScript;
        this.searchablesScript = searchablesScript;
        this.show = show;
    }

    public MetaClass getMetaClass() {
        return metaClass;
    }

    public String getSearchableIfScript() {
        return searchableIfScript;
    }

    public String getSearchablesScript() {
        return searchablesScript;
    }

    public void addProperty(String name) {
        MetaPropertyPath property = metaClass.getPropertyEx(name);

        if (property == null)
            throw new RuntimeException("Property " + name + " doesn't exist for entity " + metaClass.getName());

        if (property.getMetaProperties().length > 1 && !property.getRange().isClass())
            throw new RuntimeException("PropertyEx " + name + " must be an entity (" + metaClass.getName() + ")");

        properties.put(name, property.getRange().isClass());
    }

    public void removeProperty(String name) {
        properties.remove(name);
    }

    public Set<String> getPropertyNames() {
        return new HashSet<String>(properties.keySet());
    }

    public List<String> getLocalProperties() {
        List<String> list = new ArrayList<String>();
        for (Map.Entry<String, Boolean> entry : properties.entrySet()) {
            if (!entry.getValue())
                list.add(entry.getKey());
        }
        return list;
    }

    public List<String> getLinkProperties() {
        List<String> list = new ArrayList<String>();
        for (Map.Entry<String, Boolean> entry : properties.entrySet()) {
            if (entry.getValue())
                list.add(entry.getKey());
        }
        return list;
    }

    public boolean isShow() {
        return show;
    }
}
