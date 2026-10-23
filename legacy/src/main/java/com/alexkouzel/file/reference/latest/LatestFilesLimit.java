package com.alexkouzel.file.reference.latest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LatestFilesLimit {

    TEN(10),
    TWENTY(20),
    FORTY(40),
    EIGHTY(80),
    HUNDRED(100);

    private final int value;
}
