package com.alexkouzel.common.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.alexkouzel.common.utils.DateUtils;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;

public class DateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        String value = parser.getValueAsString();

        // Substring to avoid values such as "2023-08-16-05:00"
        if (value.length() > 10) value = value.substring(0, 10);

        try {
            return DateUtils.parse(value, "yyyy-MM-dd");
        } catch (DateTimeException e) {
            throw new IOException("Invalid date: " + value);
        }
    }
    
}
