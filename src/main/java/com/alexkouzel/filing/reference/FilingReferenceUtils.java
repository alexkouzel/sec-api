package com.alexkouzel.filing.reference;

import com.alexkouzel.common.utils.ListUtils;
import com.alexkouzel.filing.FilingType;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class FilingReferenceUtils {

    public FilingReference getFirst(List<FilingReference> refs) {
        return Collections.min(refs, Comparator.comparing(FilingReference::filedAt));
    }

    public FilingReference getLast(List<FilingReference> refs) {
        return Collections.max(refs, Comparator.comparing(FilingReference::filedAt));
    }

    public static List<FilingReference> filterAccNos(List<FilingReference> refs, Set<String> accNos) {
        return ListUtils.filter(refs, ref -> !accNos.contains(ref.accNo()));
    }

    public static List<FilingReference> filterType(List<FilingReference> refs, FilingType type) {
        return ListUtils.filter(refs, ref -> ref.type() == type);
    }

    public static Set<String> getAccNos(List<FilingReference> refs) {
        return refs.stream().map(FilingReference::accNo).collect(Collectors.toSet());
    }

    public static List<FilingReference> removeDups(List<FilingReference> refs) {
        return ListUtils.removeDups(refs, FilingReference::accNo);
    }

}
