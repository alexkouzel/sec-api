package com.alexkouzel.file.type.f345;

import com.alexkouzel.file.FileType;
import com.alexkouzel.file.FileUtils;

public class FileF3 extends FileF345 {
    public FileF3(FileF345 fileF345) throws Exception {
        super(verifyFileType(fileF345.getOwnershipDocument()));
    }

    private static OwnershipDocument verifyFileType(OwnershipDocument ownershipDocument) throws Exception {
        FileUtils.verifyEqualFileTypes(FileType.F3, ownershipDocument.type());
        return ownershipDocument;
    }
}
