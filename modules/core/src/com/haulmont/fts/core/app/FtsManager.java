/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 24.06.2010 18:18:38
 *
 * $Id$
 */
package com.haulmont.fts.core.app;

import com.haulmont.cuba.core.PersistenceProvider;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.fts.core.sys.ConfigLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import java.util.Map;
import java.util.Set;

@ManagedBean(FtsManagerAPI.NAME)
public class FtsManager implements FtsManagerAPI {

    private static Log log = LogFactory.getLog(FtsManager.class);

    private volatile Map<String, Set<String>> entities;

    private Map<String, Set<String>> getEntities() {
        if (entities == null) {
            synchronized (this) {
                if (entities == null) {
                    ConfigLoader loader = new ConfigLoader();
                    entities = loader.loadConfiguration();
                }
            }
        }
        return entities;
    }

    public boolean isSearchable(BaseEntity entity) {
        Set<String> properties = getEntities().get(entity.getClass().getName());
        if (properties == null)
            return false;

        Set<String> dirty = PersistenceProvider.getDirtyFields(entity);
        for (String s : dirty) {
            if (properties.contains(s))
                return true;
        }
        return false;
    }

}
