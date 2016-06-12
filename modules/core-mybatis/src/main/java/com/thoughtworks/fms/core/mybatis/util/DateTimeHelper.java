package com.thoughtworks.fms.core.mybatis.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateTimeHelper {
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    public static String getDefaultDateTimeStr() {
        return DEFAULT_FORMATTER.print(DateTime.now());
    }

    public static String appendDateTimeStr(String prefix, String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(separator).append(getDefaultDateTimeStr());
        return sb.toString();
    }

}
