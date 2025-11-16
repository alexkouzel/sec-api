package com.alexkouzel.file.type.f345.transaction;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public enum TransactionCode {

    // General transaction codes
    REPORTED_EARLIER("V"),
    PURCHASE("P"),
    SALE("S"),

    // Rule 16b-3 transaction codes
    SALE_BACK_TO_ISSUER("D"),
    LIABILITY_EXERCISE("F"),
    OPTION_EXERCISE("M"),
    GRANT_OR_AWARD("A"),
    DISCRETIONARY("I"),

    // Derivative securities codes (usually options)
    OPTION_EXERCISE_OUT_OF_MONEY("O"),
    OPTION_EXERCISE_IN_MONEY("X"),
    OPTION_EXPIRATION_SHORT("E"),
    OPTION_EXPIRATION_LONG("H"),
    OPTION_CONVERSION("C"),

    // Other sections 16b exempt transactions and small acquisition codes
    SMALL_ACQUISITION("L"),
    BONA_FIDE_GIFT("G"),
    VOTING_TRUST("Z"),
    INHERITED("W"),

    // Other transaction codes
    CHANGE_OF_CONTROL("U"),
    EQUITY_SWAP("K"),
    OTHER("J");

    private static final Map<String, TransactionCode> VALUE_MAP = new HashMap<>();

    public final String value;

    static {
        for (TransactionCode code : values()) {
            VALUE_MAP.put(code.value, code);
        }
    }

    public static TransactionCode ofValue(String value) {
        return VALUE_MAP.get(value);
    }
}
