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

import junit.framework.TestCase;

import java.io.StringReader;

public class EntityAttributeTokenizerTest extends TestCase {

    public void test() {
        EntityAttributeTokenizer tokenizer = new EntityAttributeTokenizer();

        boolean b = tokenizer.isTokenChar('^');
        assertTrue(b);

        b = tokenizer.isTokenChar('"');
        assertFalse(b);

        b = tokenizer.isTokenChar('$');
        assertTrue(b);
    }
}
