package com.alexkouzel.file.type.f345.owner;

import com.alexkouzel.common.deserializers.DateDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Signature {
    private String signatureName;
    @JsonDeserialize(using = DateDeserializer.class)
    private LocalDate signatureDate;
}
