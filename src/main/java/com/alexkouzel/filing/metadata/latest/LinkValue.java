package com.alexkouzel.filing.metadata.latest;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkValue {

    @JacksonXmlProperty(isAttribute = true)
    private String rel;

    @JacksonXmlProperty(isAttribute = true)
    private String href;

    @JacksonXmlProperty(isAttribute = true)
    private String type = "text/css";

}
