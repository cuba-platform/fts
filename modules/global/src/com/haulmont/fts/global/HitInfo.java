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

package com.haulmont.fts.global;

import java.io.Serializable;

/**
 * Class stores an information where the search term was found.
 * <p>
 * The {@link #fieldName} field stores the field name, e.g.:
 * <ul>
 * <li>"title"</li>
 * <li>"manager.lastName"</li>
 * <li>"document.fileContents.the.file.name" - {@link com.haulmont.cuba.core.entity.FileDescriptor} entity is a special case and the fieldName for it
 * contains a file name</li>
 * <li>"contractAttachments.document.name"</li>
 * </ul>
 * <p>
 * The {@link #highlightedText} stores a piece of field that contains a search term, e.g. "... some <b>word</b> was found ..."
 */
public class HitInfo implements Serializable {

    protected String fieldName;

    protected String highlightedText;

    public HitInfo(String fieldName, String highlightedText) {
        this.fieldName = fieldName;
        this.highlightedText = highlightedText;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getHighlightedText() {
        return highlightedText;
    }
}
