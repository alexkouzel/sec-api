package com.alexkouzel.filing.refs.cik;

import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.refs.FilingRef;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CikSubmissionParser {

    public List<FilingRef> parse(CikSubmission submission) {
        CikSubmissionFilings.Recent recent = submission.getFilings().getRecent();
        int filingNum = recent.getAccessionNumber().size();

        List<FilingRef> refs = new ArrayList<>();
        for (int i = 0; i < filingNum; i++) {

            String accNum = recent.getAccessionNumber().get(i);
            String issuerCik = submission.getCik();
            FilingType type = FilingType.ofValue(recent.getForm().get(i));
            LocalDate filedAt = recent.getFilingDate().get(i);

            FilingRef ref = new FilingRef(accNum, issuerCik, type, filedAt);
            refs.add(ref);
        }
        return refs;
    }

}
