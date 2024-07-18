package com.alexkouzel.filing.f345;

import com.alexkouzel.filing.FilingType;

import java.time.LocalDate;

public record OwnershipDocument(

        String accNum,

        FilingType type,

        LocalDate filedAt,

        String xmlFilename,

        OwnershipForm ownershipForm

) {}
