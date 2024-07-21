package com.alexkouzel.filing.reference.latest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LatestFeedCount {

    TEN(10),
    TWENTY(20),
    FORTY(40),
    EIGHTY(80),
    HUNDRED(100);

    private final int value;

}
