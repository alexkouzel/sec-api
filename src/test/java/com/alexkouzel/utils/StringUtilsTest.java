package com.alexkouzel.utils;

import com.alexkouzel.common.utils.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilsTest {

    @Test
    public void substring() {
        String result = StringUtils.substring("test <A>test<B> test", "<A>", "<B>");
        assertEquals("test", result);
    }

    @Test
    public void padLeft() {
        String result = StringUtils.padLeft("TEST", 8, 'c');
        assertEquals("ccccTEST", result);
    }

    @Test
    public void generate() {
        String result = StringUtils.generate(5, 'b');
        assertEquals("bbbbb", result);
    }

}
