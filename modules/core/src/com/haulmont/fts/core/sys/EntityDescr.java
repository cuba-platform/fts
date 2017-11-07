/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.core.sys;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MetadataTools;

import java.util.*;

public class EntityDescr {

    protected MetaClass metaClass;

    protected Map<String, Boolean> properties = new HashMap<String, Boolean>();

    protected String searchableIfScript;

    protected String searchablesScript;

    protected boolean show;

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
        MetaPropertyPath propertyPath = metaClass.getPropertyPath(name);

        if (propertyPath == null)
            throw new RuntimeException("Property " + name + " doesn't exist for entity " + metaClass.getName());

        if (propertyPath.getMetaProperties().length > 1
                && !propertyPath.getRange().isClass()
                && !AppBeans.get(MetadataTools.class).isEmbedded(propertyPath.getMetaProperties()[0]))
            throw new RuntimeException("Property " + name + " must be an entity (" + metaClass.getName() + ")");

        properties.put(name, propertyPath.getRange().isClass());
    }

    public void removeProperty(String name) {
        properties.remove(name);
    }

    public Set<String> getPropertyNames() {
        return new HashSet<>(properties.keySet());
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
