package com.alexkouzel.filing.type.f345.footnote;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FootnoteContainer {

    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Footnote> footnote;

}