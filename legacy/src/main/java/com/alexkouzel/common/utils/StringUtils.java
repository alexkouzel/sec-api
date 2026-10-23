package com.alexkouzel.common.utils;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public class StringUtils {

    public String removeFirstLine(String value) {
        int nextLineIdx = value.indexOf("\n") + 1;
        return value.substring(nextLineIdx);
    }

    public String substring(String value, String open, String close) {
        int start = value.indexOf(open);
        if (start == -1) return null;

        int end = value.indexOf(close, start + open.length());
        if (end == -1) return null;

        return value.substring(start + open.length(), end);
    }

    public String until(String value, String close) {
        int idx = value.indexOf(close);
        return value.substring(0, idx);
    }

    public String since(String value, String open) {
        int idx = value.indexOf(open);
        return value.substring(idx + open.length());
    }

    public String padLeft(String value, int length, char c) {
        String padding = generate(length - value.length(), c);
        return padding + value;
    }

    public String padRight(String value, int length, char c) {
        String padding = generate(length - value.length(), c);
        return value + padding;
    }

    public String removeLeft(String value, char c) {
        int idx = indexOfNot(value, c);
        return idx == 0 ? value : value.substring(idx);
    }

    public int indexOfNot(String value, char c) {
        int i = 0;
        while (i < value.length() && value.indexOf(i) == c) {
            i++;
        }
        return i;
    }

    public String generate(int size, char c) {
        if (size <= 0) return "";
        char[] chars = new char[size];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
