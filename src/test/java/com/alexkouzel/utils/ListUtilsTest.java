package com.alexkouzel.utils;

import com.alexkouzel.common.utils.ListUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListUtilsTest {

    @Test
    public void removeDups() {
        List<Integer> nums = List.of(1, 2, 2, 2, 3, 4);
        assertEquals(List.of(1, 2, 3, 4), ListUtils.removeDups(nums, i -> i));
    }

    // Known bug: removeDups collects into a HashMap, so order follows hash codes.
    // FileRefUtils.removeDups (public) inherits this.
    @Test
    @Tag("known-bug")
    public void removeDupsKeepsFirstSeenOrder() {
        List<Integer> nums = List.of(30, 10, 30, 20);
        assertEquals(List.of(30, 10, 20), ListUtils.removeDups(nums, i -> i));
    }
}
