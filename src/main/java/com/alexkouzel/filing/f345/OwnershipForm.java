package com.alexkouzel.filing.f345;

import com.alexkouzel.filing.f345.owner.ReportingOwner;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.alexkouzel.filing.deserializers.BooleanDeserializer;
import com.alexkouzel.filing.deserializers.EdgarDateDeserializer;
import com.alexkouzel.filing.f345.footnote.FootnoteContainer;
import com.alexkouzel.filing.f345.owner.Issuer;
import com.alexkouzel.filing.f345.owner.Signature;
import com.alexkouzel.filing.f345.transaction.derivative.DerivativeTable;
import com.alexkouzel.filing.f345.transaction.nonderivative.NonDerivativeTable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class OwnershipForm {

    private String schemaVersion;

    private String documentType;

    private String aff10b5One;

    private NonDerivativeTable nonDerivativeTable;

    private DerivativeTable derivativeTable;

    private Issuer issuer;

    @JsonDeserialize(using = EdgarDateDeserializer.class)
    private LocalDate periodOfReport;

    @JsonDeserialize(using = EdgarDateDeserializer.class)
    private LocalDate dateOfOriginalSubmission;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean noSecuritiesOwned;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean notSubjectToSection16;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean form3HoldingsReported;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean form4TransactionsReported;

    @JacksonXmlElementWrapper(useWrapping = false)
    private List<ReportingOwner> reportingOwner;

    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Signature> ownerSignature;

    private FootnoteContainer footnotes;

    private String remarks;

}