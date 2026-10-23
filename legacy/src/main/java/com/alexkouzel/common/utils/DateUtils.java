package com.alexkouzel.common.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@UtilityClass
public class DateUtils {

    public LocalDate parse(String value, String pattern) throws DateTimeParseException {
        return LocalDate.parse(value, getFormatter(pattern));
    }

    private DateTimeFormatter getFormatter(String pattern) {
        ZoneId zoneId = ZoneId.of("UTC");
        return DateTimeFormatter.ofPattern(pattern).withZone(zoneId);
    }
}
