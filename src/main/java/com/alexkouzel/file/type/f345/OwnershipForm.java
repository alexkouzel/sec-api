package com.alexkouzel.file.type.f345;

import com.alexkouzel.common.deserializers.BooleanDeserializer;
import com.alexkouzel.common.deserializers.DateDeserializer;
import com.alexkouzel.file.type.f345.footnote.FootnoteContainer;
import com.alexkouzel.file.type.f345.owner.Issuer;
import com.alexkouzel.file.type.f345.owner.ReportingOwner;
import com.alexkouzel.file.type.f345.owner.Signature;
import com.alexkouzel.file.type.f345.transaction.derivative.DerivativeTable;
import com.alexkouzel.file.type.f345.transaction.nonderivative.NonDerivativeTable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
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
    @JsonDeserialize(using = DateDeserializer.class)
    private LocalDate periodOfReport;
    @JsonDeserialize(using = DateDeserializer.class)
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
