package com.alexkouzel.filing.reference.daily;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.reference.FilingReference;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class DailyFeedParser {

    public List<FilingReference> parse(String data) throws ParsingException {
        String hr = StringUtils.substring(data, "<hr>", "<hr>");
        if (hr == null) throw new ParsingException("<hr> field is missing");

        List<FilingReference> result = new ArrayList<>();
        for (String entry : hr.split("\n")) {
            result.add(parseEntry(entry));
        }
        return result;
    }

    private FilingReference parseEntry(String entry) {
        String[] parts = entry.split("</a>");

        // Parse issuer CIK
        int issuerCikIdx = parts[1].indexOf(">") + 1;
        String issuerCikValue = parts[1].substring(issuerCikIdx);
        int issuerCik = Integer.parseInt(issuerCikValue);

        // Parse accession number
        int accNoIdx = parts[0].indexOf(issuerCikValue) + issuerCikValue.length() + 1;
        String accNo = parts[0].substring(accNoIdx, parts[0].indexOf("-index"));

        // Parse filing type
        int typeIdx = parts[0].indexOf(">") + 1;
        String typeValue = parts[0].substring(typeIdx);
        FilingType type = FilingType.ofValue(typeValue);

        // Parse filing date
        String dateValue = entry.substring(0, entry.indexOf(" "));
        LocalDate filedAt = DateUtils.parse(dateValue, "MM-dd-yyyy");

        return new FilingReference(accNo, issuerCik, type, filedAt);
    }
}
