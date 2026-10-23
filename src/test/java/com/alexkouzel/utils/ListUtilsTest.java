package com.alexkouzel.utils;

import com.alexkouzel.common.utils.ListUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListUtilsTest {

    @Test
    public void removeDups() {
        List<Integer> nums = List.of(1, 2, 2, 2, 3, 4);
        assertEquals(List.of(1, 2, 3, 4), ListUtils.removeDups(nums, i -> i));
    }

    @Test
    public void removeDupsKeepsFirstSeenOrder() {
        List<Integer> nums = List.of(30, 10, 30, 20);
        assertEquals(List.of(30, 10, 20), ListUtils.removeDups(nums, i -> i));
    }
}
