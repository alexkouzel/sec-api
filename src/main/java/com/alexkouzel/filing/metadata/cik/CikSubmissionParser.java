package com.alexkouzel.filing.metadata.cik;

import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.metadata.FilingMetadata;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CikSubmissionParser {

    public List<FilingMetadata> parse(CikSubmission submission) {
        CikSubmissionFilings.Recent recent = submission.getFilings().getRecent();
        int filingCount = recent.getAccessionNumber().size();

        List<FilingMetadata> result = new ArrayList<>();
        for (int i = 0; i < filingCount; i++) {

            String accNo = recent.getAccessionNumber().get(i);
            String issuerCik = submission.getCik();
            FilingType type = FilingType.ofValue(recent.getForm().get(i));
            LocalDate filedAt = recent.getFilingDate().get(i);

            FilingMetadata metadata = new FilingMetadata(accNo, issuerCik, type, filedAt);
            result.add(metadata);
        }
        return result;
    }

}
