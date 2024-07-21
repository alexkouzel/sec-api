package com.alexkouzel.filing.type.f345.footnote;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FootnoteValue<T> {

    private FootnoteID footnoteId;

    private T value;

    public FootnoteValue(T value, String id) {
        this.value = value;
        this.footnoteId = new FootnoteID(id);
    }

    public FootnoteValue(T value) {
        this.value = value;
    }

    public FootnoteValue() {
    }

}
