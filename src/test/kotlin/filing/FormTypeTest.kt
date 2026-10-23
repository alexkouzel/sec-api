package com.alexkouzel.sec.filing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormTypeTest {
    @Test
    fun `equals a constant with the same value`() {
        assertEquals(FormType.F4_A, FormType("4/A"))
    }

    @Test
    fun `keeps forms without a constant`() {
        assertEquals("424B2", FormType("424B2").toString())
    }

    @Test
    fun `an amendment ends in slash A`() {
        assertTrue(FormType("SCHEDULE 13D/A").isAmendment)
        assertFalse(FormType.F4.isAmendment)
    }
}
