package com.alexkouzel.filing.refs.daily;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.refs.FilingRef;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.common.utils.StringUtils;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class DailyFeedParser {

    public List<FilingRef> parse(String data) throws ParsingException {
        String hr = StringUtils.substring(data, "<hr>", "<hr>");
        if (hr == null) throw new ParsingException("<hr> field is missing");

        List<FilingRef> refs = new ArrayList<>();
        for (String entry : hr.split("\n")) {
            refs.add(parseEntry(entry));
        }
        return refs;
    }

    private FilingRef parseEntry(String entry) {
        String[] parts = entry.split("</a>");

        // Parse issuer CIK
        int issuerCikIdx = parts[1].indexOf(">") + 1;
        String issuerCik = parts[1].substring(issuerCikIdx);

        // Parse accession number
        int accNumIdx = parts[0].indexOf(issuerCik) + issuerCik.length() + 1;
        String accNum = parts[0].substring(accNumIdx, parts[0].indexOf("-index"));

        // Parse filing type
        int typeIdx = parts[0].indexOf(">") + 1;
        String typeValue = parts[0].substring(typeIdx);
        FilingType type = FilingType.ofValue(typeValue);

        // Parse filing date
        String dateValue = entry.substring(0, entry.indexOf(" "));
        LocalDate filedAt = DateUtils.parse(dateValue, "MM-dd-yyyy");

        return new FilingRef(accNum, issuerCik, type, filedAt);
    }

}
