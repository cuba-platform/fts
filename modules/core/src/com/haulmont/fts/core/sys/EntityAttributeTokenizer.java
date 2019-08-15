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

import com.haulmont.fts.global.FTS;
import org.apache.lucene.analysis.util.CharTokenizer;

public class EntityAttributeTokenizer extends CharTokenizer {

    public EntityAttributeTokenizer() {
        super();
    }

    protected int normalize(int c) {
        return Character.toLowerCase(c);
    }

    @Override
    protected boolean isTokenChar(int c) {
        return FTS.isTokenChar(c);
    }
}