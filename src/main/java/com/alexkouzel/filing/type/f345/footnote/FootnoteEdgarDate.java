package com.alexkouzel.filing.type.f345.footnote;

import com.alexkouzel.filing.deserializers.EdgarDateDeserializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.alexkouzel.common.utils.DateUtils;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FootnoteEdgarDate {

    private FootnoteID footnoteId;

    @JsonDeserialize(using = EdgarDateDeserializer.class)
    private LocalDate value;

    @JsonCreator
    public FootnoteEdgarDate(@JsonProperty("footnoteId") FootnoteID footnoteId,
                             @JsonProperty("value") LocalDate value) {
        this.footnoteId = footnoteId;
        this.value = value;
    }

    public FootnoteEdgarDate(String value) {
        this.value = DateUtils.parse(value, "yyyy-MM-dd");
    }

}
