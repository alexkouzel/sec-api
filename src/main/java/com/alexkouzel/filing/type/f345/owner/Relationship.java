package com.alexkouzel.filing.type.f345.owner;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.alexkouzel.common.deserializers.BooleanDeserializer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Relationship {

    @JsonDeserialize(using = BooleanDeserializer.class)
    private boolean isDirector;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private boolean isOfficer;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private boolean isTenPercentOwner;

    @JsonDeserialize(using = BooleanDeserializer.class)
    private boolean isOther;

    private String officerTitle;

    private String otherText;

}