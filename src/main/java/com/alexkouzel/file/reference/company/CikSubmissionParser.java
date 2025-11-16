package com.alexkouzel.file.reference.company;

import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CikSubmissionParser {

    public List<FileRef> parse(CikSubmission submission) {
        CikSubmissionFilings.Recent recent = submission.getFilings().getRecent();
        int filingCount = recent.getAccessionNumber().size();

        List<FileRef> fileRefs = new ArrayList<>();
        for (int i = 0; i < filingCount; i++) {

            String accNo = recent.getAccessionNumber().get(i);
            String typeValue = recent.getForm().get(i);
            LocalDate filedAt = recent.getFilingDate().get(i);

            int issuerCik = submission.getCik();
            FileType type = FileType.ofValue(typeValue);

            FileRef fileRef = new FileRef(accNo, issuerCik, type, filedAt);
            fileRefs.add(fileRef);
        }
        return fileRefs;
    }
}
