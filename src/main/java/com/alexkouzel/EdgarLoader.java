package com.alexkouzel;

import com.alexkouzel.client.DataClient;
import com.alexkouzel.client.EdgarClient;
import com.alexkouzel.client.exceptions.RequestFailedException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.company.Company;
import com.alexkouzel.company.CompanyLoader;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.f345.OwnershipDoc;
import com.alexkouzel.filing.f345.OwnershipDocLoader;
import com.alexkouzel.filing.refs.FilingRef;
import com.alexkouzel.filing.refs.FilingRefLoader;

import java.util.List;

public class EdgarLoader {

    private final CompanyLoader companyLoader;

    private final OwnershipDocLoader ownershipDocLoader;

    private final FilingRefLoader refLoader;

    public EdgarLoader(DataClient client) {
        this.companyLoader = new CompanyLoader(client);
        this.ownershipDocLoader = new OwnershipDocLoader(client);
        this.refLoader = new FilingRefLoader(client);
    }

    public EdgarLoader(String userAgent) {
        this(new EdgarClient(userAgent));
    }

    public List<Company> loadCompanies()
            throws RequestFailedException, ParsingException {
        return companyLoader.loadCompanies();
    }

    public OwnershipDoc loadOwnershipDoc(FilingRef ref)
            throws RequestFailedException, ParsingException {
        return ownershipDocLoader.loadByRef(ref);
    }

    public List<FilingRef> loadRefsDaysAgo(FilingType filingType, int daysAgo)
            throws RequestFailedException, ParsingException {
        return refLoader.loadDaysAgo(filingType, daysAgo);
    }

    public List<FilingRef> loadRefsByQuarter(FilingType filingType, int year, int quarter)
            throws RequestFailedException, ParsingException {
        return refLoader.loadByQuarter(filingType, year, quarter);
    }

    public List<FilingRef> loadLatestRefs(FilingType filingType, int start, int count)
            throws RequestFailedException, ParsingException {
        return refLoader.loadLatest(filingType, start, count);
    }

    public List<FilingRef> loadRefsByCik(FilingType filingType, String cik)
            throws RequestFailedException, ParsingException {
        return refLoader.loadByCik(filingType, cik);
    }

}
