package com.alexkouzel.file.type.f345;

import com.alexkouzel.file.FileType;

import java.time.LocalDate;

public record OwnershipDocument(
        String accNo,
        FileType type,
        LocalDate filedAt,
        String xmlFilename,
        OwnershipForm ownershipForm
) {}
