package com.alexkouzel.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum FileType {

    F3("3"),        // initial insider transactions
    F3_A("3/A"),    // initial insider transactions (amendment)
    F4("4"),        // insider transactions
    F4_A("4/A"),    // insider transactions (amendment)
    F5("5"),        // annual insider transactions that weren't reported in form 4
    F5_A("5/A"),    // annual insider transactions that weren't reported in form 4 (amendment)

    Q10("10-Q"),    // quarterly financial reports
    K10("10-K"),    // annual financial reports
    K8("8-K"),      // events important to shareholders

    OTHER("-");

    private static final Map<String, FileType> VALUE_MAP = new HashMap<>();

    private final String value;

    static {
        for (FileType type : values()) {
            VALUE_MAP.put(type.value, type);
        }
    }

    public static FileType ofValue(String value) {
        return VALUE_MAP.getOrDefault(value, FileType.OTHER);
    }
}
