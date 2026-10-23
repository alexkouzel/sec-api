package com.alexkouzel.drift;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Records unknown JSON fields and XML elements instead of failing on them, so one
 * run lists every addition. Mappers are set up like the library's own.
 */
public class UnknownFieldCollector extends DeserializationProblemHandler {
    private final Set<String> fields = new TreeSet<>();

    public ObjectMapper jsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        configure(mapper);
        return mapper;
    }

    public XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();
        configure(mapper);
        return mapper;
    }

    private void configure(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.addHandler(this);
    }

    @Override
    public boolean handleUnknownProperty(DeserializationContext context, JsonParser parser,
                                         JsonDeserializer<?> deserializer, Object beanOrClass,
                                         String propertyName) throws IOException {
        Class<?> type = beanOrClass instanceof Class<?> c ? c : beanOrClass.getClass();
        String typeName = type.getName().substring(type.getPackageName().length() + 1).replace('$', '.');
        fields.add(typeName + "." + propertyName);
        parser.skipChildren();
        return true;
    }

    /** Fields seen since the last call, e.g. {@code ReportingOwner.rptOwnerNewThing}. */
    public Set<String> drain() {
        Set<String> seen = new TreeSet<>(fields);
        fields.clear();
        return seen;
    }
}
