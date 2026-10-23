package com.alexkouzel.sec.ownership.xml

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.filing.FormType
import com.alexkouzel.sec.ownership.FilingText
import com.alexkouzel.sec.ownership.Footnoted
import com.alexkouzel.sec.ownership.TransactionCode
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversionTest {
    private fun filing(accessionNumber: String) = FilingText.parse(Fixtures.bytes("ownership/$accessionNumber.txt"))

    private fun date(value: String) = LocalDate.parse(value)

    @Test
    fun `form 4 with a sale and indirect holdings`() {
        val filing = filing("0001127602-23-028345")

        assertEquals("NSP", filing.issuer.ticker)
        assertEquals(date("2023-11-27"), filing.periodOfReport)
        assertEquals(false, filing.isRule10b5One)

        val owner = filing.owners.single()
        assertEquals(1055610, owner.cik)
        assertEquals("RAWSON RICHARD G", owner.name)
        assertEquals("KINGWOOD", owner.address?.city)
        assertTrue(owner.relationship.isDirector)
        assertFalse(owner.relationship.isOfficer)

        val sale = filing.transactions.single()
        assertNull(sale.derivative)
        assertEquals(Footnoted("Common Stock"), sale.securityTitle)
        assertEquals(Footnoted(date("2023-11-27")), sale.date)
        assertEquals(TransactionCode.SALE, sale.coding?.code)
        assertEquals(false, sale.coding?.isEquitySwap)
        assertEquals(Footnoted(3500.0), sale.shares)
        assertEquals(Footnoted(114.36, listOf("F1")), sale.pricePerShare)
        assertEquals(Footnoted("D"), sale.acquiredOrDisposed)
        assertEquals(Footnoted(1925.0), sale.sharesOwnedAfter)
        assertEquals(Footnoted("D"), sale.directOrIndirect)
        assertNull(sale.timeliness)

        assertEquals(listOf("F1"), filing.footnotes.map { it.id })
        assertTrue(
            filing.footnotes
                .single()
                .text
                .startsWith("Weighted average price."),
        )
        assertEquals("/s/ Christian P. Callens, by Power of Attorney", filing.signatures.single().name)
        assertEquals(date("2023-11-28"), filing.signatures.single().date)
    }

    // This filing interleaves holdings and a transaction (holding x3, transaction, holding),
    // which the XSD allows.
    @Test
    fun `keeps every interleaved holding`() {
        val filing = filing("0001127602-23-028345")

        assertEquals(4, filing.holdings.size)
        val holding = filing.holdings.first()
        assertEquals(Footnoted(84216.0), holding.sharesOwned)
        assertEquals(Footnoted("I"), holding.directOrIndirect)
        assertEquals(
            "By Self, as General Partner of POM Services, LLC, which is the General Partner of R&D Rawson, LP",
            holding.natureOfOwnership?.value,
        )
    }

    @Test
    fun `form 4 with an option exercise`() {
        val filing = filing("0001701247-26-000014")

        assertEquals(true, filing.isRule10b5One)
        val relationship = filing.owners.first().relationship
        assertFalse(relationship.isDirector)
        assertTrue(relationship.isOfficer)
        assertEquals("CHIEF MEDICAL OFFICER", relationship.officerTitle)

        val (stock, options) = filing.transactions.partition { it.derivative == null }
        assertEquals(2, stock.size)
        val exercise = stock.first()
        assertEquals(TransactionCode.OPTION_EXERCISE, exercise.coding?.code)
        assertEquals(5000.0, exercise.shares?.value)
        assertEquals(2.48, exercise.pricePerShare?.value)
        assertEquals("A", exercise.acquiredOrDisposed?.value)
        assertEquals(30000.0, exercise.sharesOwnedAfter?.value)

        val option = options.single()
        assertEquals("Stock Option (right to buy)", option.securityTitle?.value)
        assertEquals(2.48, option.derivative?.exercisePrice?.value)
        assertEquals(date("2026-10-19"), option.date?.value)
        assertEquals(TransactionCode.OPTION_EXERCISE, option.coding?.code)
        assertEquals(5000.0, option.shares?.value)
        assertEquals(0.0, option.pricePerShare?.value)
        assertEquals("D", option.acquiredOrDisposed?.value)
        assertEquals(date("2031-06-17"), option.derivative?.expirationDate?.value)
        assertEquals("Common Stock", option.derivative?.underlyingTitle?.value)
        assertEquals(5000.0, option.derivative?.underlyingShares?.value)
        assertEquals(101268.0, option.sharesOwnedAfter?.value)

        assertEquals(listOf("F1", "F2", "F3", "F4", "F5"), filing.footnotes.map { it.id })
    }

    @Test
    fun `form 3 with many owners and holdings`() {
        val filing = filing("0000921895-26-002730")

        assertEquals("Foghorn Therapeutics Inc.", filing.issuer.name)
        assertEquals(date("2026-10-01"), filing.periodOfReport)
        assertFalse(filing.ownsNoSecurities)

        assertEquals(10, filing.owners.size)
        val owner = filing.owners.first()
        assertEquals(1055947, owner.cik)
        assertEquals("BVF PARTNERS L P/IL", owner.name)
        assertTrue(owner.relationship.isTenPercentOwner)
        assertEquals(false, owner.address?.isNonUS)

        assertEquals(emptyList(), filing.transactions)
        val (stock, warrants) = filing.holdings.partition { it.derivative == null }
        assertEquals(3, stock.size)
        assertEquals(4494462.0, stock.first().sharesOwned?.value)
        assertEquals(9, warrants.size)
        val warrant = warrants.first()
        assertEquals("Pre-Funded Warrants to Purchase Common Stock", warrant.securityTitle?.value)
        assertEquals("Common Stock, $0.0001 par value", warrant.derivative?.underlyingTitle?.value)
        assertEquals(3132723.0, warrant.derivative?.underlyingShares?.value)

        assertEquals(listOf("F1", "F2", "F3", "F4", "F5", "F6", "F7"), filing.footnotes.map { it.id })
    }

    @Test
    fun `form 3 with no securities owned`() {
        val filing = filing("0000929638-26-003820")

        assertTrue(filing.ownsNoSecurities)
        assertEquals(emptyList(), filing.transactions)
        assertEquals(emptyList(), filing.holdings)
        assertEquals(emptyList(), filing.footnotes)
        val relationship = filing.owners.first().relationship
        assertTrue(relationship.isOther)
        assertFalse(relationship.isDirector)
    }

    @Test
    fun `form 3 amendment with an option holding`() {
        val filing = filing("0001193125-26-427843")

        assertEquals(FormType.F3_A, filing.formType)
        assertEquals(date("2026-09-03"), filing.dateOfOriginalSubmission)
        assertEquals(
            "Chief Technology Officer",
            filing.owners
                .first()
                .relationship.officerTitle,
        )

        val option = filing.holdings.first { it.derivative != null }
        assertEquals("Stock Option (right to buy)", option.securityTitle?.value)
        assertEquals(0.47, option.derivative?.exercisePrice?.value)
        assertEquals(date("2034-09-09"), option.derivative?.expirationDate?.value)
        assertEquals(3217784.0, option.derivative?.underlyingShares?.value)
    }

    @Test
    fun `form 4 with a non-US owner`() {
        val filing = filing("0001493152-26-046274")

        val address = filing.owners.first().address
        assertEquals(true, address?.isNonUS)
        assertEquals("F4", address?.country)
        assertEquals("BEIJING", address?.city)

        val purchase = filing.transactions.first()
        assertEquals("American depositary shares", purchase.securityTitle?.value)
        assertEquals(TransactionCode.PURCHASE, purchase.coding?.code)
        assertEquals(5505.0, purchase.shares?.value)
        assertEquals(1.2128, purchase.pricePerShare?.value)
        assertNull(purchase.deemedExecutionDate)
    }

    @Test
    fun `a footnote can stand in for a value`() {
        val filing = filing("0001887616-26-000024")

        assertEquals(date("2026-10-02"), filing.dateOfOriginalSubmission)
        assertEquals(6, filing.transactions.count { it.derivative == null })
        assertEquals(3, filing.transactions.count { it.derivative != null })
        val first = filing.transactions.first()
        assertEquals(Footnoted(33967.0), first.shares)
        assertEquals(Footnoted(null, listOf("F1")), first.pricePerShare)
    }

    @Test
    fun `keeps several footnotes on one value`() {
        val filing = filing("0001000045-22-000004")

        assertEquals(Footnoted(2262.0, listOf("F1", "F2")), filing.transactions.first().shares)
    }

    @Test
    fun `keeps footnotes on a transaction coding`() {
        val coding = filing("0001315098-26-000186").transactions.first().coding

        assertEquals(TransactionCode.SALE, coding?.code)
        assertEquals(listOf("F1"), coding?.footnoteIds)
    }

    @Test
    fun `form 5 with fractional shares`() {
        val filing = filing("0001225208-26-008370")

        assertTrue(
            filing.owners
                .first()
                .relationship.isOther,
        )
        assertEquals(12, filing.transactions.size)
        val first = filing.transactions.first()
        assertEquals(date("2025-10-30"), first.date?.value)
        assertEquals(TransactionCode.OTHER, first.coding?.code)
        assertEquals(24.4, first.shares?.value)
        assertEquals(20.68, first.pricePerShare?.value)
        assertEquals(3794.908, first.sharesOwnedAfter?.value)
    }

    @Test
    fun `keeps line breaks in footnote text`() {
        val footnote = filing("0001437749-22-017271").footnotes.single { it.id == "F3" }

        assertContains(footnote.text, "has the effect of\ncorrecting the Form 4")
    }

    @Test
    fun `form 5 amendment with a gift`() {
        val filing = filing("0001437749-22-017271")

        assertEquals(date("2022-06-14"), filing.dateOfOriginalSubmission)
        val gift = filing.transactions.first()
        assertEquals(TransactionCode.GIFT, gift.coding?.code)
        assertEquals(date("2021-09-29"), gift.date?.value)
        assertEquals(2000.0, gift.shares?.value)
    }
}
