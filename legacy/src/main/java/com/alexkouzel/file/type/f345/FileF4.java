package com.alexkouzel.file.type.f345;

import com.alexkouzel.file.FileType;
import com.alexkouzel.file.FileUtils;

public class FileF4 extends FileF345 {
    public FileF4(FileF345 fileF345) throws Exception {
        super(verifyFileType(fileF345.getOwnershipDocument()));
    }

    private static OwnershipDocument verifyFileType(OwnershipDocument ownershipDocument) throws Exception {
        FileUtils.verifyEqualFileTypes(FileType.F4, ownershipDocument.type());
        return ownershipDocument;
    }
}
