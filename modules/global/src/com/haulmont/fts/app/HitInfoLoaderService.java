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

package com.haulmont.fts.app;

import com.haulmont.fts.global.HitInfo;
import com.haulmont.fts.global.SearchResult;

import java.util.List;

/**
 * Service that loads and creates {@link HitInfo}
 */
public interface HitInfoLoaderService {

    String NAME = "cuba_HitInfoLoaderService";

    /**
     * Method fills the {@code hitInfos} collection of each {@link com.haulmont.fts.global.SearchResultEntry} of the {@link SearchResult}
     */
    void populateHitInfos(SearchResult searchResult);

    /**
     * Method builds hit infos collection for a single entity. The method queries lucene index for the content of the document related to the entity.
     */
    List<HitInfo> loadHitInfos(String entityName, Object entityId, String searchTerm);
}
