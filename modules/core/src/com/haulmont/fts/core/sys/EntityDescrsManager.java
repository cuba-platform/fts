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

/**
 * Class is used for getting an information about what entities and their attributes must be indexed from the FTS
 * configuration file
 */
public interface EntityDescrsManager {

    String NAME = "fts_EntityDescrsManager";

    Map<String, EntityDescr> getDescrByNameMap();

    @Nullable
    EntityDescr getDescrByEntityName(String entityName);

    /**
     * Finds the {@link EntityDescr object} by the metaclass. If the result for the given metaclass is not found, then a
     * search for the original metaclass of the given class is performed
     */
    @Nullable
    EntityDescr getDescrByMetaClass(MetaClass metaClass);

    Map<String, EntityDescr> getDescrByClassNameMap();
}
