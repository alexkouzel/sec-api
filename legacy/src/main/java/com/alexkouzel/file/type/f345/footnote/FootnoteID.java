package com.alexkouzel.file.type.f345.footnote;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class FootnoteID {
    @JacksonXmlProperty(isAttribute = true)
    private String id;

    public static List<FootnoteID> of(String... ids) {
        return Arrays.stream(ids).map(FootnoteID::new).toList();
    }
}
