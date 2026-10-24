package com.alexkouzel.sec.ownership

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransactionTest {
    @Test
    fun `a coding's code comes from its value`() {
        val coding =
            Coding(formType = null, value = "S", isEquitySwap = false, footnoteIds = emptyList())

        assertEquals(TransactionCode.SALE, coding.code)
    }

    @Test
    fun `an unknown code is null and keeps its value`() {
        val coding =
            Coding(formType = null, value = "Q", isEquitySwap = false, footnoteIds = emptyList())

        assertNull(coding.code)
        assertEquals("Q", coding.value)
    }
}
