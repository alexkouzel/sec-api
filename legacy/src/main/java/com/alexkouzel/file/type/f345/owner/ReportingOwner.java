package com.alexkouzel.file.type.f345.owner;

import com.alexkouzel.common.deserializers.BooleanDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportingOwner {
    private ID reportingOwnerId;
    private Address reportingOwnerAddress;
    private Relationship reportingOwnerRelationship;

    @Getter
    @Setter
    public static class ID {
        private int rptOwnerCik;
        private String rptOwnerCcc;
        private String rptOwnerName;
    }

    @Getter
    @Setter
    public static class Address {
        @JsonDeserialize(using = BooleanDeserializer.class)
        private Boolean rptOwnerNonUSAddressFlag;
        private String rptOwnerStreet1;
        private String rptOwnerStreet2;
        private String rptOwnerCity;
        private String rptOwnerState;
        private String rptOwnerZipCode;
        private String rptOwnerStateDescription;
        private String rptOwnerNonUSStateTerritory;
        private String rptOwnerCountry;
        @JsonDeserialize(using = BooleanDeserializer.class)
        private Boolean rptOwnerGoodAddress;
    }
}
