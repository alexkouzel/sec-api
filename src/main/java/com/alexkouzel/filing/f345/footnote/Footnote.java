package com.alexkouzel.filing.f345.footnote;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Footnote {

    @JacksonXmlText
    private String value;

    @JacksonXmlProperty(isAttribute = true)
    private String id;

}