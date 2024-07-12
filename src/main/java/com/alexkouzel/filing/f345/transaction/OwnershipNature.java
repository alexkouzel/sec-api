package com.alexkouzel.filing.f345.transaction;

import com.alexkouzel.filing.f345.footnote.FootnoteValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnershipNature {

    private FootnoteValue<String> directOrIndirectOwnership;

    private FootnoteValue<String> natureOfOwnership;

}
