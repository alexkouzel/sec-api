package com.alexkouzel.file.type.f345.footnote;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A value with the footnotes attached to it. Either part may be missing: the XSD allows
 * a footnote in place of a value, and up to 99 footnote IDs per value.
 */
@Getter
@Setter
public class FootnoteValue<T> {
    private T value;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "footnoteId")
    @JsonMerge
    private List<FootnoteID> footnoteIds;

    public FootnoteValue(T value, String... footnoteIds) {
        this.value = value;
        this.footnoteIds = new ArrayList<>(FootnoteID.of(footnoteIds));
    }

    public FootnoteValue() {
    }
}
