package com.alexkouzel.file.reference.company;

import com.alexkouzel.common.deserializers.BooleanDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CikSubmission {
    private int cik;
    private String entityType;
    private String sic;
    private String sicDescription;
    private String ownerOrg;
    @JsonDeserialize(using = BooleanDeserializer.class)
    private boolean insiderTransactionForOwnerExists;
    @JsonDeserialize(using = BooleanDeserializer.class)
    private boolean insiderTransactionForIssuerExists;
    private String name;
    private List<String> tickers;
    private List<String> exchanges;
    private int ein;
    private String lei;
    private String description;
    private String website;
    private String investorWebsite;
    private String category;
    private String fiscalYearEnd;
    private String stateOfIncorporation;
    private String stateOfIncorporationDescription;
    private Map<String, Address> addresses;
    private String phone;
    private String flags;
    private List<FormerName> formerNames;
    private CikSubmissionFilings filings;

    @Getter
    @Setter
    public static class Address {
        private String street1;
        private String street2;
        private String city;
        private String stateOrCountry;
        private String zipCode;
        private String stateOrCountryDescription;
        @JsonDeserialize(using = BooleanDeserializer.class)
        private Boolean isForeignLocation;
        private String foreignStateTerritory;
        private String country;
        private String countryCode;
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
