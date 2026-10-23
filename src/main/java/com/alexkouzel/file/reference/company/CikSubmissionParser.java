package com.alexkouzel.file.reference.company;

import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CikSubmissionParser {

    /**
     * Parses one block of filings: {@code filings.recent} of a submission, or one of
     * the older files listed in {@code filings.files} (same shape).
     */
    public List<FileRef> parse(CikSubmissionFilings.Recent filings, int cik) {
        int filingCount = filings.getAccessionNumber().size();

        List<FileRef> fileRefs = new ArrayList<>();
        for (int i = 0; i < filingCount; i++) {

            String accNo = filings.getAccessionNumber().get(i);
            String typeValue = filings.getForm().get(i);
            LocalDate filedAt = filings.getFilingDate().get(i);

            FileType type = FileType.ofValue(typeValue);

            FileRef fileRef = new FileRef(accNo, cik, type, filedAt);
            fileRefs.add(fileRef);
        }
        return fileRefs;
    }
}
