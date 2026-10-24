package com.alexkouzel.sec;

import com.alexkouzel.sec.filing.FilingRef;
import com.alexkouzel.sec.filing.FormType;
import com.alexkouzel.sec.ownership.OwnershipFiling;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// The main paths, called from Java as a Java user would.
class JavaLiveTest {

    @Test
    void mainPathsWorkFromJava() {
        String userAgent = System.getenv()
                .getOrDefault("EDGAR_USER_AGENT", "TestCompany test.company@gmail.com");

        try (SecClient sec = new SecClient(userAgent)) {
            assertFalse(sec.getCompanies().all().isEmpty());
            assertFalse(sec.getFilings().latest(40).isEmpty());

            List<FilingRef> refs = sec.getFilings().daily(1, FormType.F4);
            assumeFalse(refs.isEmpty(), "no Form 4 filed one day ago");
            OwnershipFiling filing = sec.getOwnership().get(refs.get(0));

            assertEquals(FormType.F4, filing.getFormType());
            assertFalse(filing.getOwners().isEmpty());
        }
    }
}
