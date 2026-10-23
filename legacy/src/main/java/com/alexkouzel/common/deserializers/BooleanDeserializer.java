package com.alexkouzel.common.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class BooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        String value = parser.getValueAsString();
        if ("1".equals(value) || "true".equals(value)) {
            return true;
        }
        if ("0".equals(value) || "false".equals(value)) {
            return false;
        }
        throw new IOException("Invalid value for boolean: " + value);
    }
}
