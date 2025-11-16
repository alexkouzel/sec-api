package com.alexkouzel.file.type.f345;

import com.alexkouzel.file.File;
import lombok.Getter;

@Getter
public class FileF345 extends File {
    private final OwnershipDocument ownershipDocument;

    public FileF345(OwnershipDocument ownershipDocument) {
        super(ownershipDocument.type());
        this.ownershipDocument = ownershipDocument;
    }
}
