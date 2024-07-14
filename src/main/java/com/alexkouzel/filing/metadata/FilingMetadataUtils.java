package com.alexkouzel.filing.metadata;

import com.alexkouzel.common.utils.ListUtils;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class FilingMetadataUtils {

    public FilingMetadata getFirst(List<FilingMetadata> metadata) {
        return Collections.min(metadata, Comparator.comparing(FilingMetadata::filedAt));
    }

    public FilingMetadata getLast(List<FilingMetadata> metadata) {
        return Collections.max(metadata, Comparator.comparing(FilingMetadata::filedAt));
    }

    public static List<FilingMetadata> filterAccNums(List<FilingMetadata> metadata, Set<String> accNums) {
        return ListUtils.filter(metadata, entry -> !accNums.contains(entry.accNum()));
    }

    public static Set<String> getAccNums(List<FilingMetadata> metadata) {
        return metadata.stream().map(FilingMetadata::accNum).collect(Collectors.toSet());
    }

    public static List<FilingMetadata> removeDups(List<FilingMetadata> metadata) {
        return ListUtils.removeDups(metadata, FilingMetadata::accNum);
    }

}
