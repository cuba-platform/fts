/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.global;

import com.google.common.base.Joiner;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.datatypes.FormatStrings;
import com.haulmont.cuba.core.global.*;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

        } else if (value instanceof Enum) {
            return formatEnum((Enum) value);

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

    private String formatEnum(Enum enumValue) {
        Messages messages = AppBeans.get(Messages.class);
        GlobalConfig globalConfig = AppBeans.get(Configuration.class).getConfig(GlobalConfig.class);
        Set<String> localizedValues = new HashSet<>();
        for (Locale locale : globalConfig.getAvailableLocales().values()) {
            localizedValues.add(messages.getMessage(enumValue, locale));
        }
        return Joiner.on(" ").join(localizedValues);
    }

    private Object tryDate(String value) {
        UserSessionSource userSession = AppBeans.get(UserSessionSource.NAME);
        FormatStrings formatStrings = Datatypes.getFormatStrings(userSession.getLocale());
        SimpleDateFormat sdf = new SimpleDateFormat(formatStrings.getDateFormat());
        try {
            Date date = sdf.parse(value);
            return date;
        } catch (ParseException e) {
            return null;
        }
    }

    private Object tryNumber(String value) {
        UserSessionSource userSession = AppBeans.get(UserSessionSource.NAME);
        FormatStrings formatStrings = Datatypes.getFormatStrings(userSession.getLocale());
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
