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
