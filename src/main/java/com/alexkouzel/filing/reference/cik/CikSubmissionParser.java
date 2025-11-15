package com.alexkouzel.filing.reference.cik;

import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.reference.FilingReference;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CikSubmissionParser {

    public List<FilingReference> parse(CikSubmission submission) {
        CikSubmissionFilings.Recent recent = submission.getFilings().getRecent();
        int filingCount = recent.getAccessionNumber().size();

        List<FilingReference> refs = new ArrayList<>();
        for (int i = 0; i < filingCount; i++) {

            String accNo = recent.getAccessionNumber().get(i);
            String typeValue = recent.getForm().get(i);
            LocalDate filedAt = recent.getFilingDate().get(i);

            int issuerCik = submission.getCik();
            FilingType type = FilingType.ofValue(typeValue);

            FilingReference ref = new FilingReference(accNo, issuerCik, type, filedAt);
            refs.add(ref);
        }
        return refs;
    }
}
