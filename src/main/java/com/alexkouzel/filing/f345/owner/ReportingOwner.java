package com.alexkouzel.filing.f345.owner;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.alexkouzel.filing.deserializers.BooleanDeserializer;
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

        private String rptOwnerCik;

        private String rptOwnerCcc;

        private String rptOwnerName;

    }

    @Getter
    @Setter
    public static class Address {

        private String rptOwnerStreet1;

        private String rptOwnerStreet2;

        private String rptOwnerCity;

        private String rptOwnerState;

        private String rptOwnerZipCode;

        private String rptOwnerStateDescription;

        @JsonDeserialize(using = BooleanDeserializer.class)
        private Boolean rptOwnerGoodAddress;

    }

}