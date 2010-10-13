/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 26.07.2010 17:32:17
 *
 * $Id$
 */
package com.haulmont.fts.core.sys;

import com.haulmont.cuba.core.SecurityProvider;
import com.haulmont.cuba.core.global.MessageUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import java.math.BigDecimal;
import java.text.*;
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
        if (value == null)
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
        String userDateFormat = MessageUtils.getDateFormat();
        SimpleDateFormat sdf = new SimpleDateFormat(userDateFormat);
        try {
            Date date = sdf.parse(value);
            return date;
        } catch (ParseException e) {
            return null;
        }
    }

    private Object tryNumber(String value) {
        char decimalSeparator = '.';
        char groupingSeparator = ',';
        NumberFormat f = NumberFormat.getInstance(SecurityProvider.currentUserSession().getLocale());
        if (f instanceof DecimalFormat) {
            decimalSeparator = ((DecimalFormat) f).getDecimalFormatSymbols().getDecimalSeparator();
            groupingSeparator = ((DecimalFormat) f).getDecimalFormatSymbols().getGroupingSeparator();
        }
        if (decimalSeparator != '.')
            value = value.replace(decimalSeparator, '.');
        if (groupingSeparator != ',')
            value = value.replace(groupingSeparator, ',');

        try {
            Number number = new BigDecimal(value);
            return number;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
