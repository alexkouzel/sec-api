package com.alexkouzel.file.type.f345.transaction;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class TransactionCodeDeserializer extends JsonDeserializer<TransactionCode> {

    @Override
    public TransactionCode deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        String value = parser.getValueAsString();
        return TransactionCode.ofValue(value);
    }
}
