package com.alexkouzel.filing.metadata;

import com.alexkouzel.common.utils.ListUtils;
import com.alexkouzel.filing.FilingType;
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

    public static List<FilingMetadata> filterAccNos(List<FilingMetadata> metadata, Set<String> accNos) {
        return ListUtils.filter(metadata, entry -> !accNos.contains(entry.accNo()));
    }

    public static List<FilingMetadata> filterType(List<FilingMetadata> metadata, FilingType type) {
        return ListUtils.filter(metadata, entry -> entry.type() == type);
    }

    public static Set<String> getAccNos(List<FilingMetadata> metadata) {
        return metadata.stream().map(FilingMetadata::accNo).collect(Collectors.toSet());
    }

    public static List<FilingMetadata> removeDups(List<FilingMetadata> metadata) {
        return ListUtils.removeDups(metadata, FilingMetadata::accNo);
    }

}
