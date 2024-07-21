package com.alexkouzel.filing.type.f345.owner;

import com.alexkouzel.filing.deserializers.EdgarDateDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Signature {

    private String signatureName;

    @JsonDeserialize(using = EdgarDateDeserializer.class)
    private LocalDate signatureDate;

}