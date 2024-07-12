package com.alexkouzel.common.utils;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class ListUtils {

    public <T, U> List<T> removeDups(List<T> list, Function<T, U> keyMapper) {
        return new ArrayList<>(list.stream()
                .collect(Collectors.toMap(keyMapper, Function.identity(), (e1, e2) -> e1))
                .values());
    }

    public <T> List<T> filter(List<T> list, Predicate<T> filter) {
        return list.stream().filter(filter).collect(Collectors.toList());
    }

}
