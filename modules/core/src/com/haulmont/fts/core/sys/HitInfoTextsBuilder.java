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

import java.util.Map;

public interface HitInfoTextsBuilder {

    String NAME = "fts_HitTextsBuilder";

    /**
     * Method builds pieces of text where the search term is highlighted
     * @param fieldAllContent the content of the "all" field of the lucene document. The "all" field content may be something like this:
     *                        "^^name John Doe ^^address The Main Street"
     * @param searchTerm the search term
     * @return a map. The key is the field name where the search term is found, the value is highlighted text
     */
    Map<String, String> buildHighlightedHitTexts(String fieldAllContent, String searchTerm);
}
