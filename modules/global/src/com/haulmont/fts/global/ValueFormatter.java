/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 26.07.2010 17:32:17
 *
 * $Id$
 */
package com.haulmont.fts.global;

import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.datatypes.FormatStrings;
import com.haulmont.cuba.core.global.UserSessionProvider;
import org.apache.commons.lang.time.DateFormatUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ValueFormatter {

    public String format(Object value) {
        if (value == null)
            return null;

        if (value instanceof String) {
            return (String) value;

        } else if (value instanceof Date) {
            return DateFormatUtils.format((Date) value, "yyyy-MM-dd");

        } else if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
            DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
            formatSymbols.setDecimalSeparator('_');
            DecimalFormat decimalFormat = new DecimalFormat("#.####", formatSymbols);
            return decimalFormat.format(value);

        } else if (value instanceof Integer || value instanceof Long) {
            return value.toString();

        } else
            return value.toString();
    }

    public String guessTypeAndFormat(String value) {
        if (value == null || value.length() == 0)
            return null;

        Object v = tryDate(value);
        if (v != null)
            return format(v);

        v = tryNumber(value);
        if (v != null)
            return format(v);

        return value;
    }

    private Object tryDate(String value) {
        FormatStrings formatStrings = Datatypes.getFormatStrings(UserSessionProvider.getLocale());
        SimpleDateFormat sdf = new SimpleDateFormat(formatStrings.getDateFormat());
        try {
            Date date = sdf.parse(value);
            return date;
        } catch (ParseException e) {
            return null;
        }
    }

    private Object tryNumber(String value) {
        FormatStrings formatStrings = Datatypes.getFormatStrings(UserSessionProvider.getLocale());
        char decimalSeparator = formatStrings.getFormatSymbols().getDecimalSeparator();
        char groupingSeparator = formatStrings.getFormatSymbols().getGroupingSeparator();
        if (decimalSeparator != '.')
            value = value.replace(decimalSeparator, '.');
        if (groupingSeparator != ',')
            value = value.replace(groupingSeparator, ',');

        if (!Character.isDigit(value.charAt(0)) || value.startsWith("0"))
            return null;

        try {
            Number number = new BigDecimal(value);
            return number;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
