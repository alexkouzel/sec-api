package com.alexkouzel.file.reference;

import com.alexkouzel.common.utils.ListUtils;
import com.alexkouzel.file.FileType;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class FileRefUtils {

    public FileRef getFirst(List<FileRef> fileRefs) {
        return Collections.min(fileRefs, Comparator.comparing(FileRef::filedAt));
    }

    public FileRef getLast(List<FileRef> fileRefs) {
        return Collections.max(fileRefs, Comparator.comparing(FileRef::filedAt));
    }

    public static List<FileRef> filterAccNos(List<FileRef> fileRefs, Set<String> accNos) {
        return ListUtils.filter(fileRefs, fileRef -> !accNos.contains(fileRef.accNo()));
    }

    public static List<FileRef> filterType(List<FileRef> fileRefs, FileType type) {
        return ListUtils.filter(fileRefs, fileRef -> fileRef.type() == type);
    }

    public static Set<String> getAccNos(List<FileRef> fileRefs) {
        return fileRefs.stream().map(FileRef::accNo).collect(Collectors.toSet());
    }

    public static List<FileRef> removeDups(List<FileRef> fileRefs) {
        return ListUtils.removeDups(fileRefs, FileRef::accNo);
    }
}
