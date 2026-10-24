package com.alexkouzel.sec;

import com.alexkouzel.sec.filing.FilingRef;
import com.alexkouzel.sec.filing.Filings;
import com.alexkouzel.sec.filing.FormType;
import com.alexkouzel.sec.ownership.Ownership;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The public API as a Java user sees it. Compiling this is most of the test: it breaks if a
// Kotlin change makes the API awkward or unusable from Java.
class JavaApiTest {

    @Test
    void rejectsAUserAgentWithoutAContact() {
        assertThrows(IllegalArgumentException.class, () -> new SecClient("Sample Company"));
    }

    @Test
    void checksArgumentsBeforeAnyRequest() {
        try (SecClient sec = new SecClient("Sample Company admin@example.com")) {
            Filings filings = sec.getFilings();
            Ownership ownership = sec.getOwnership();

            assertThrows(IllegalArgumentException.class, () -> filings.latest(50));
            assertThrows(IllegalArgumentException.class, () -> filings.latest(50, FormType.F4));
            assertThrows(IllegalArgumentException.class, () -> filings.day(-1));
            assertThrows(IllegalArgumentException.class, () -> filings.quarter(2026, 5));
            assertThrows(IllegalArgumentException.class, () -> ownership.get(1000753, "0001127602"));
        }
    }

    @Test
    void readsModelsThroughGetters() {
        FilingRef ref = new FilingRef("0001127602-23-028345", 1000753, FormType.F4, LocalDate.parse("2023-11-28"));

        assertEquals("4", ref.getFormType().getValue());
        assertEquals(1000753, ref.getCik());
        assertTrue(FormType.F4_A.isAmendment());
    }
}
