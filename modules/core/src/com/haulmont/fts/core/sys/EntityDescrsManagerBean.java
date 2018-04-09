/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.Metadata;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Component(EntityDescrsManager.NAME)
public class EntityDescrsManagerBean implements EntityDescrsManager {

    @Inject
    protected ConfigLoader configLoader;

    @Inject
    protected Metadata metadata;

    protected volatile Map<String, EntityDescr> descrByClassNameMap;
    protected volatile Map<String, EntityDescr> descrByNameMap;

    @Override
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

    @Override
    @Nullable
    public EntityDescr getDescrByEntityName(String entityName) {
        Map<String, EntityDescr> descrByNameMap = getDescrByNameMap();
        return descrByNameMap.get(entityName);
    }

    @Override
    @Nullable
    public EntityDescr getDescrByMetaClass(MetaClass metaClass) {
        EntityDescr descr = getDescrByEntityName(metaClass.getName());
        if (descr == null) {
            MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(metaClass);
            if (originalMetaClass != null)
                descr = getDescrByEntityName(originalMetaClass.getName());
        }
        return descr;
    }

    @Override
    public Map<String, EntityDescr> getDescrByClassNameMap() {
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
