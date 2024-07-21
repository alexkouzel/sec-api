package com.alexkouzel.filing.reference.cik;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.alexkouzel.common.deserializers.BooleanDeserializer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CikSubmission {

    private String cik;

    private String entityType;

    private String sic;

    private String sicDescription;

    private String name;

    private String description;

    private String website;

    private String investorWebsite;

    private String category;

    private String stateOfIncorporation;

    private String stateOfIncorporationDescription;

    private String fiscalYearEnd;

    private String phone;

    private String flags;

    private CikSubmissionFilings filings;

    private List<String> tickers;

    private List<String> exchanges;

    private List<FormerName> formerNames;

    private Map<String, Address> addresses;

    private int ein;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private boolean insiderTransactionForOwnerExists;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private boolean insiderTransactionForIssuerExists;

    @Getter
    @Setter
    public static class Address {

        private String street1;

        private String street2;

        private String city;

        private String stateOrCountry;

        private String zipCode;

        private String stateOrCountryDescription;

    }

    @Getter
    @Setter
    public static class FormerName {

        private String name;

        private LocalDate from;

        private LocalDate to;
    }

    @Getter
    @Setter
    public static class FilingFile {

        private String name;

        private LocalDate filingFrom;

        private LocalDate filingTo;

        private int filingCount;

    }

}
