package com.alexkouzel.file.type.f345.footnote;

import com.alexkouzel.common.deserializers.DateDeserializer;
import com.alexkouzel.common.utils.DateUtils;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * A date with the footnotes attached to it; see {@link FootnoteValue}.
 */
@Getter
@Setter
@NoArgsConstructor
public class FootnoteEdgarDate {
    @JsonDeserialize(using = DateDeserializer.class)
    private LocalDate value;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "footnoteId")
    @JsonMerge
    private List<FootnoteID> footnoteIds;

    // Used when the element holds text instead of <value> and <footnoteId>: a bare date,
    // or nothing at all (e.g. <deemedExecutionDate></deemedExecutionDate>), which means absent.
    public static FootnoteEdgarDate valueOf(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        FootnoteEdgarDate date = new FootnoteEdgarDate();
        date.setValue(DateUtils.parse(text.trim(), "yyyy-MM-dd"));
        return date;
    }
}
