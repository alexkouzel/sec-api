package com.alexkouzel.file;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileUtils {

    public void verifyEqualFileTypes(FileType fileType1, FileType fileType2) throws Exception {
        if (fileType1 != fileType2) {
            throw new Exception("File types are not equal");
        }
    }
}
