package com.alexkouzel.sec.drift

import kotlin.test.Test
import kotlin.test.assertEquals

class DriftCheckTest {
    @Test
    fun `names a raw response after its URL`() {
        assertEquals(
            "cgi-bin_current_q1_1_q3_4",
            rawName("https://www.sec.gov/cgi-bin/current?q1=1&q3=4"),
        )
        assertEquals(
            "Archives_edgar_data_1000753_000112760223028345_0001127602-23-028345.txt",
            rawName("https://www.sec.gov/Archives/edgar/data/1000753/000112760223028345/0001127602-23-028345.txt"),
        )
    }
}
