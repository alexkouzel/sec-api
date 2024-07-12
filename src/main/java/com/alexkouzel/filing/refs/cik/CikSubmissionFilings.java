package com.alexkouzel.filing.refs.cik;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.alexkouzel.filing.deserializers.BooleanDeserializer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CikSubmissionFilings {

    private Recent recent;

    private List<CikSubmission.FilingFile> files;

    @Getter
    @Setter
    public static class Recent {

        private List<String> accessionNumber;

        private List<String> act;

        private List<String> form;

        private List<String> fileNumber;

        private List<String> filmNumber;

        private List<String> items;

        private List<String> primaryDocument;

        private List<String> primaryDocDescription;

        private List<LocalDate> filingDate;

        private List<LocalDate> reportDate;

        private List<LocalDate> acceptanceDateTime;

        private List<Integer> size;

        @JsonDeserialize(contentUsing = BooleanDeserializer.class)
        private List<Boolean> isXBRL;

        @JsonDeserialize(contentUsing = BooleanDeserializer.class)
        private List<Boolean> isInlineXBRL;

    }

}
