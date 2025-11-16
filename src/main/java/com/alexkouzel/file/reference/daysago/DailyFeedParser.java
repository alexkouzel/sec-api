package com.alexkouzel.file.reference.daysago;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class DailyFeedParser {

    public List<FileRef> parse(String data) throws ParsingException {
        String hr = StringUtils.substring(data, "<hr>", "<hr>");
        if (hr == null) {
            throw new ParsingException("<hr> field is missing");
        }
        List<FileRef> result = new ArrayList<>();
        for (String entry : hr.split("\n")) {
            result.add(parseEntry(entry));
        }
        return result;
    }

    private FileRef parseEntry(String entry) {
        String[] parts = entry.split("</a>");

        // Parse issuer CIK
        int issuerCikIdx = parts[1].indexOf(">") + 1;
        String issuerCikValue = parts[1].substring(issuerCikIdx);
        int issuerCik = Integer.parseInt(issuerCikValue);

        // Parse accession number
        int accNoIdx = parts[0].indexOf(issuerCikValue) + issuerCikValue.length() + 1;
        String accNo = parts[0].substring(accNoIdx, parts[0].indexOf("-index"));

        // Parse file type
        int typeIdx = parts[0].indexOf(">") + 1;
        String typeValue = parts[0].substring(typeIdx);
        FileType type = FileType.ofValue(typeValue);

        // Parse file date
        String dateValue = entry.substring(0, entry.indexOf(" "));
        LocalDate filedAt = DateUtils.parse(dateValue, "MM-dd-yyyy");

        return new FileRef(accNo, issuerCik, type, filedAt);
    }
}
