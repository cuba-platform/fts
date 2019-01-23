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

import com.haulmont.fts.global.ValueFormatter;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ValueFormatterTest extends TestCase {

    public void testFormat() throws Exception {
        ValueFormatter valueFormatter = new ValueFormatter();

        BigDecimal bigDecimal = new BigDecimal("12345.67");
        String str = valueFormatter.format(bigDecimal);
        assertEquals("12345_67", str);

        Double dbl = 12345.67;
        str = valueFormatter.format(dbl);
        assertEquals("12345_67", str);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = dateFormat.parse("2010-07-26 17:58:11");
        str = valueFormatter.format(date);
        assertEquals("2010-07-26", str);
    }
}
