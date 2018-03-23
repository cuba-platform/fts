/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Class is used for getting an information about what entities and their attributes must be indexed from the FTS
 * configuration file
 */
@Component("fts_EntityDescrsManager")
public class EntityDescrsManager {

    @Inject
    protected ConfigLoader configLoader;

    protected volatile Map<String, EntityDescr> descrByClassNameMap;
    protected volatile Map<String, EntityDescr> descrByNameMap;

    public Map<String, EntityDescr> getDescrByNameMap() {
        if (descrByNameMap == null) {
            synchronized (this) {
                if (descrByNameMap == null) {
                    descrByNameMap = new HashMap<>(getDescrByClassNameMap().size());
                    for (EntityDescr descr : getDescrByClassNameMap().values()) {
                        String name = descr.getMetaClass().getName();
                        descrByNameMap.put(name, descr);
                    }
                }
            }
        }
        return descrByNameMap;
    }

    public EntityDescr getDescrByEntityName(String entityName) {
        Map<String, EntityDescr> descrByNameMap = getDescrByNameMap();
        return descrByNameMap.get(entityName);
    }

    protected Map<String, EntityDescr> getDescrByClassNameMap() {
        if (descrByClassNameMap == null) {
            synchronized (this) {
                if (descrByClassNameMap == null) {
                    descrByClassNameMap = configLoader.loadConfiguration();
                }
            }
        }
        return descrByClassNameMap;
    }
}
