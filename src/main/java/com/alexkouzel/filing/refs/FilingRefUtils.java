package com.alexkouzel.filing.refs;

import com.alexkouzel.common.utils.ListUtils;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class FilingRefUtils {

    public FilingRef getFirst(List<FilingRef> refs) {
        return Collections.min(refs, Comparator.comparing(FilingRef::filedAt));
    }

    public FilingRef getLast(List<FilingRef> refs) {
        return Collections.max(refs, Comparator.comparing(FilingRef::filedAt));
    }

    public static List<FilingRef> filterAccNums(List<FilingRef> refs, Set<String> accNums) {
        return ListUtils.filter(refs, ref -> !accNums.contains(ref.accNum()));
    }

    public static Set<String> getAccNums(List<FilingRef> refs) {
        return refs.stream().map(FilingRef::accNum).collect(Collectors.toSet());
    }

    public static List<FilingRef> removeDups(List<FilingRef> refs) {
        return ListUtils.removeDups(refs, FilingRef::accNum);
    }

}
