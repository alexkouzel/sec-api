package com.alexkouzel.file.type.f345;

import com.alexkouzel.file.FileLoader;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.FileUrlBuilder;
import com.alexkouzel.file.reference.FileRef;
import com.alexkouzel.file.type.f345.footnote.Footnote;
import com.alexkouzel.file.type.f345.footnote.FootnoteID;
import com.alexkouzel.file.type.f345.footnote.FootnoteValue;
import com.alexkouzel.file.type.f345.owner.ReportingOwner;
import com.alexkouzel.file.type.f345.transaction.TransactionCode;
import com.alexkouzel.file.type.f345.transaction.derivative.DerivativeHolding;
import com.alexkouzel.file.type.f345.transaction.derivative.DerivativeTransaction;
import com.alexkouzel.file.type.f345.transaction.nonderivative.NonDerivativeHolding;
import com.alexkouzel.file.type.f345.transaction.nonderivative.NonDerivativeTransaction;
import com.alexkouzel.testing.FixtureClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnershipDocumentLoaderTest {
    private final FixtureClient client = new FixtureClient();
    private final OwnershipDocumentLoader loader = new OwnershipDocumentLoader(client);

    private String route(int issuerCik, String accNo) {
        String url = FileUrlBuilder.buildTxt(issuerCik, accNo);
        client.route(url, "ownership/" + accNo + ".txt");
        return url;
    }

    private OwnershipForm loadForm(int issuerCik, String accNo) throws Exception {
        return loader.loadByUrl(route(issuerCik, accNo)).ownershipForm();
    }

    private static LocalDate date(String value) {
        return LocalDate.parse(value);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "0000921895-26-002730, 1822462, 3,   2026-10-05, X0607, form307422fhtx_10052026.xml",
            "0000929638-26-003820, 2097545, 3,   2026-10-02, X0607, form3.xml",
            "0001000045-22-000004, 1000045, 4,   2022-09-01, X0306, primary_doc.xml",
            "0001062993-22-016446, 1000623, 3,   2022-07-18, X0206, form3.xml",
            "0001062993-26-005298, 1099369, 5,   2026-10-13, X0609, form5.xml",
            "0001127602-23-028345, 1000753, 4,   2023-11-28, X0508, form4.xml",
            "0001193125-26-427843, 2111860, 3/A, 2026-10-21, X0607, ownership.xml",
            "0001225208-26-008370, 1300391, 5,   2026-10-13, X0609, doc5.xml",
            "0001315098-26-000186, 1315098, 4,   2026-10-02, X0609, wk-form4_1790978110.xml",
            "0001437749-22-017271,   69891, 5/A, 2022-07-18, X0306, rdgdoc.xml",
            "0001437749-22-020350,  100378, 5,   2022-08-12, X0306, rdgdoc.xml",
            "0001493152-26-046274, 1729173, 4,   2026-10-08, X0609, ownership.xml",
            "0001701247-26-000014, 1672619, 4,   2026-10-21, X0609, form4-10212026_091032.xml",
            "0001887616-26-000024, 1819989, 4/A, 2026-10-08, X0609, wk-form4a_1791500258.xml",
    })
    public void parsesHeaderAndForm(String accNo, int issuerCik, String type, String filedAt,
                                    String schemaVersion, String xmlFilename) throws Exception {
        OwnershipDocument doc = loader.loadByUrl(route(issuerCik, accNo));

        assertEquals(accNo, doc.accNo());
        assertEquals(FileType.ofValue(type), doc.type());
        assertEquals(date(filedAt), doc.filedAt());
        assertEquals(xmlFilename, doc.xmlFilename());

        OwnershipForm form = doc.ownershipForm();
        assertEquals(schemaVersion, form.getSchemaVersion());
        assertEquals(type, form.getDocumentType());
        assertEquals(issuerCik, form.getIssuer().getIssuerCik());
        assertFalse(form.getReportingOwner().isEmpty());
    }

    @Test
    public void loadByFileRefKeepsRefMetadata() throws Exception {
        FileRef ref = new FileRef("0001127602-23-028345", 1000753, FileType.F4, date("2023-11-28"));
        route(ref.issuerCik(), ref.accNo());

        OwnershipDocument doc = loader.loadByFileRef(ref);

        assertEquals(ref.accNo(), doc.accNo());
        assertEquals(ref.type(), doc.type());
        assertEquals(ref.filedAt(), doc.filedAt());
        assertEquals("form4.xml", doc.xmlFilename());
        assertEquals("INSPERITY, INC.", doc.ownershipForm().getIssuer().getIssuerName());
    }

    @Test
    public void fileLoaderWrapsDocument() throws Exception {
        FileRef ref = new FileRef("0001127602-23-028345", 1000753, FileType.F4, date("2023-11-28"));
        route(ref.issuerCik(), ref.accNo());

        FileF345 file = new FileLoader(client).loadF345ByRef(ref);

        assertEquals(FileType.F4, file.getFileType());
        assertEquals(ref.accNo(), file.getOwnershipDocument().accNo());
    }

    @Test
    public void form4WithSaleAndIndirectHoldings() throws Exception {
        OwnershipForm form = loadForm(1000753, "0001127602-23-028345");

        assertEquals("NSP", form.getIssuer().getIssuerTradingSymbol());
        assertEquals(date("2023-11-27"), form.getPeriodOfReport());
        assertEquals("0", form.getAff10b5One());

        ReportingOwner owner = form.getReportingOwner().getFirst();
        assertEquals(1055610, owner.getReportingOwnerId().getRptOwnerCik());
        assertEquals("RAWSON RICHARD G", owner.getReportingOwnerId().getRptOwnerName());
        assertEquals("KINGWOOD", owner.getReportingOwnerAddress().getRptOwnerCity());
        assertTrue(owner.getReportingOwnerRelationship().isDirector());
        assertFalse(owner.getReportingOwnerRelationship().isOfficer());

        List<NonDerivativeTransaction> transactions = form.getNonDerivativeTable().getNonDerivativeTransaction();
        assertEquals(1, transactions.size());
        NonDerivativeTransaction sale = transactions.getFirst();
        assertEquals("Common Stock", sale.getSecurityTitle().getValue());
        assertEquals(date("2023-11-27"), sale.getTransactionDate().getValue());
        assertEquals(TransactionCode.SALE, sale.getTransactionCoding().getTransactionCode());
        assertFalse(sale.getTransactionCoding().isEquitySwapInvolved());
        assertEquals(3500.0, sale.getTransactionAmounts().getTransactionShares().getValue());
        assertEquals(114.36, sale.getTransactionAmounts().getTransactionPricePerShare().getValue());
        assertEquals("D", sale.getTransactionAmounts().getTransactionAcquiredDisposedCode().getValue());
        assertEquals(1925.0, sale.getPostTransactionAmounts().getSharesOwnedFollowingTransaction().getValue());
        assertEquals("D", sale.getOwnershipNature().getDirectOrIndirectOwnership().getValue());

        assertEquals(List.of("F1"), footnoteIds(form));
        assertEquals("/s/ Christian P. Callens, by Power of Attorney", form.getOwnerSignature().getFirst().getSignatureName());
        assertEquals(date("2023-11-28"), form.getOwnerSignature().getFirst().getSignatureDate());
    }

    // This filing interleaves holdings and a transaction (holding x3, transaction, holding),
    // which the XSD allows.
    @Test
    public void interleavedHoldingsAreAllKept() throws Exception {
        OwnershipForm form = loadForm(1000753, "0001127602-23-028345");

        List<NonDerivativeHolding> holdings = form.getNonDerivativeTable().getNonDerivativeHolding();
        assertEquals(4, holdings.size());
        NonDerivativeHolding holding = holdings.getFirst();
        assertEquals(84216.0, holding.getPostTransactionAmounts().getSharesOwnedFollowingTransaction().getValue());
        assertEquals("I", holding.getOwnershipNature().getDirectOrIndirectOwnership().getValue());
        assertEquals("By Self, as General Partner of POM Services, LLC, which is the General Partner of R&D Rawson, LP",
                holding.getOwnershipNature().getNatureOfOwnership().getValue());
    }

    @Test
    public void form4WithOptionExercise() throws Exception {
        OwnershipForm form = loadForm(1672619, "0001701247-26-000014");

        assertEquals("1", form.getAff10b5One());
        ReportingOwner owner = form.getReportingOwner().getFirst();
        assertFalse(owner.getReportingOwnerRelationship().isDirector());
        assertTrue(owner.getReportingOwnerRelationship().isOfficer());
        assertEquals("CHIEF MEDICAL OFFICER", owner.getReportingOwnerRelationship().getOfficerTitle());

        List<NonDerivativeTransaction> nonDerivative = form.getNonDerivativeTable().getNonDerivativeTransaction();
        assertEquals(2, nonDerivative.size());
        NonDerivativeTransaction exercise = nonDerivative.getFirst();
        assertEquals(TransactionCode.OPTION_EXERCISE, exercise.getTransactionCoding().getTransactionCode());
        assertEquals(5000.0, exercise.getTransactionAmounts().getTransactionShares().getValue());
        assertEquals(2.48, exercise.getTransactionAmounts().getTransactionPricePerShare().getValue());
        assertEquals("A", exercise.getTransactionAmounts().getTransactionAcquiredDisposedCode().getValue());
        assertEquals(30000.0, exercise.getPostTransactionAmounts().getSharesOwnedFollowingTransaction().getValue());

        List<DerivativeTransaction> derivative = form.getDerivativeTable().getDerivativeTransaction();
        assertEquals(1, derivative.size());
        DerivativeTransaction option = derivative.getFirst();
        assertEquals("Stock Option (right to buy)", option.getSecurityTitle().getValue());
        assertEquals(2.48, option.getConversionOrExercisePrice().getValue());
        assertEquals(date("2026-10-19"), option.getTransactionDate().getValue());
        assertEquals(TransactionCode.OPTION_EXERCISE, option.getTransactionCoding().getTransactionCode());
        assertEquals(5000.0, option.getTransactionAmounts().getTransactionShares().getValue());
        assertEquals(0.0, option.getTransactionAmounts().getTransactionPricePerShare().getValue());
        assertEquals("D", option.getTransactionAmounts().getTransactionAcquiredDisposedCode().getValue());
        assertEquals(date("2031-06-17"), option.getExpirationDate().getValue());
        assertEquals("Common Stock", option.getUnderlyingSecurity().getUnderlyingSecurityTitle().getValue());
        assertEquals(5000.0, option.getUnderlyingSecurity().getUnderlyingSecurityShares().getValue());
        assertEquals(101268.0, option.getPostTransactionAmounts().getSharesOwnedFollowingTransaction().getValue());

        assertEquals(List.of("F1", "F2", "F3", "F4", "F5"), footnoteIds(form));
    }

    @Test
    public void form3WithManyOwnersAndHoldings() throws Exception {
        OwnershipForm form = loadForm(1822462, "0000921895-26-002730");

        assertEquals("Foghorn Therapeutics Inc.", form.getIssuer().getIssuerName());
        assertEquals(date("2026-10-01"), form.getPeriodOfReport());
        assertFalse(form.getNoSecuritiesOwned());

        assertEquals(10, form.getReportingOwner().size());
        ReportingOwner owner = form.getReportingOwner().getFirst();
        assertEquals(1055947, owner.getReportingOwnerId().getRptOwnerCik());
        assertEquals("BVF PARTNERS L P/IL", owner.getReportingOwnerId().getRptOwnerName());
        assertTrue(owner.getReportingOwnerRelationship().isTenPercentOwner());
        assertFalse(owner.getReportingOwnerAddress().getRptOwnerNonUSAddressFlag());

        assertNull(form.getNonDerivativeTable().getNonDerivativeTransaction());
        assertEquals(3, form.getNonDerivativeTable().getNonDerivativeHolding().size());
        assertEquals(4494462.0, form.getNonDerivativeTable().getNonDerivativeHolding().getFirst()
                .getPostTransactionAmounts().getSharesOwnedFollowingTransaction().getValue());

        List<DerivativeHolding> derivative = form.getDerivativeTable().getDerivativeHolding();
        assertEquals(9, derivative.size());
        DerivativeHolding warrants = derivative.getFirst();
        assertEquals("Pre-Funded Warrants to Purchase Common Stock", warrants.getSecurityTitle().getValue());
        assertEquals("Common Stock, $0.0001 par value", warrants.getUnderlyingSecurity().getUnderlyingSecurityTitle().getValue());
        assertEquals(3132723.0, warrants.getUnderlyingSecurity().getUnderlyingSecurityShares().getValue());

        assertEquals(List.of("F1", "F2", "F3", "F4", "F5", "F6", "F7"), footnoteIds(form));
    }

    @Test
    public void form3WithNoSecuritiesOwned() throws Exception {
        OwnershipForm form = loadForm(2097545, "0000929638-26-003820");

        assertTrue(form.getNoSecuritiesOwned());
        assertNull(form.getNonDerivativeTable());
        // The filing has an empty <derivativeTable></derivativeTable>.
        assertNull(form.getDerivativeTable().getDerivativeTransaction());
        assertNull(form.getDerivativeTable().getDerivativeHolding());
        assertNull(form.getFootnotes());

        ReportingOwner owner = form.getReportingOwner().getFirst();
        assertTrue(owner.getReportingOwnerRelationship().isOther());
        assertFalse(owner.getReportingOwnerRelationship().isDirector());
    }

    @Test
    public void form3AmendmentWithOptionHolding() throws Exception {
        OwnershipForm form = loadForm(2111860, "0001193125-26-427843");

        assertEquals("3/A", form.getDocumentType());
        assertEquals(date("2026-09-03"), form.getDateOfOriginalSubmission());
        assertEquals("Chief Technology Officer",
                form.getReportingOwner().getFirst().getReportingOwnerRelationship().getOfficerTitle());

        DerivativeHolding option = form.getDerivativeTable().getDerivativeHolding().getFirst();
        assertEquals("Stock Option (right to buy)", option.getSecurityTitle().getValue());
        assertEquals(0.47, option.getConversionOrExercisePrice().getValue());
        assertEquals(date("2034-09-09"), option.getExpirationDate().getValue());
        assertEquals(3217784.0, option.getUnderlyingSecurity().getUnderlyingSecurityShares().getValue());
    }

    @Test
    public void form4WithNonUsOwner() throws Exception {
        OwnershipForm form = loadForm(1729173, "0001493152-26-046274");

        ReportingOwner.Address address = form.getReportingOwner().getFirst().getReportingOwnerAddress();
        assertTrue(address.getRptOwnerNonUSAddressFlag());
        assertEquals("F4", address.getRptOwnerCountry());
        assertEquals("BEIJING", address.getRptOwnerCity());

        NonDerivativeTransaction purchase = form.getNonDerivativeTable().getNonDerivativeTransaction().getFirst();
        assertEquals("American depositary shares", purchase.getSecurityTitle().getValue());
        assertEquals(TransactionCode.PURCHASE, purchase.getTransactionCoding().getTransactionCode());
        assertEquals(5505.0, purchase.getTransactionAmounts().getTransactionShares().getValue());
        assertEquals(1.2128, purchase.getTransactionAmounts().getTransactionPricePerShare().getValue());
        // <deemedExecutionDate></deemedExecutionDate> means absent.
        assertNull(purchase.getDeemedExecutionDate());
    }

    @Test
    public void form4AmendmentWithFootnotedPrice() throws Exception {
        OwnershipForm form = loadForm(1819989, "0001887616-26-000024");

        assertEquals(date("2026-10-02"), form.getDateOfOriginalSubmission());
        assertEquals(6, form.getNonDerivativeTable().getNonDerivativeTransaction().size());
        assertEquals(3, form.getDerivativeTable().getDerivativeTransaction().size());

        NonDerivativeTransaction first = form.getNonDerivativeTable().getNonDerivativeTransaction().getFirst();
        assertEquals(33967.0, first.getTransactionAmounts().getTransactionShares().getValue());
        assertNull(first.getTransactionAmounts().getTransactionPricePerShare().getValue());
        assertEquals(FootnoteID.of("F1"), first.getTransactionAmounts().getTransactionPricePerShare().getFootnoteIds());
    }

    @Test
    public void valueWithSeveralFootnotes() throws Exception {
        OwnershipForm form = loadForm(1000045, "0001000045-22-000004");

        FootnoteValue<Double> shares = form.getNonDerivativeTable().getNonDerivativeTransaction().getFirst()
                .getTransactionAmounts().getTransactionShares();
        assertEquals(2262.0, shares.getValue());
        assertEquals(FootnoteID.of("F1", "F2"), shares.getFootnoteIds());
    }

    @Test
    public void transactionCodingFootnote() throws Exception {
        OwnershipForm form = loadForm(1315098, "0001315098-26-000186");

        NonDerivativeTransaction sale = form.getNonDerivativeTable().getNonDerivativeTransaction().getFirst();
        assertEquals(TransactionCode.SALE, sale.getTransactionCoding().getTransactionCode());
        assertEquals(FootnoteID.of("F1"), sale.getTransactionCoding().getFootnoteIds());
    }

    @Test
    public void form5WithFractionalShares() throws Exception {
        OwnershipForm form = loadForm(1300391, "0001225208-26-008370");

        assertTrue(form.getReportingOwner().getFirst().getReportingOwnerRelationship().isOther());
        List<NonDerivativeTransaction> transactions = form.getNonDerivativeTable().getNonDerivativeTransaction();
        assertEquals(12, transactions.size());
        NonDerivativeTransaction first = transactions.getFirst();
        assertEquals(date("2025-10-30"), first.getTransactionDate().getValue());
        assertEquals(TransactionCode.OTHER, first.getTransactionCoding().getTransactionCode());
        assertEquals(24.4, first.getTransactionAmounts().getTransactionShares().getValue());
        assertEquals(20.68, first.getTransactionAmounts().getTransactionPricePerShare().getValue());
        assertEquals(3794.908, first.getPostTransactionAmounts().getSharesOwnedFollowingTransaction().getValue());
    }

    @Test
    public void form5AmendmentWithGift() throws Exception {
        OwnershipForm form = loadForm(69891, "0001437749-22-017271");

        assertEquals(date("2022-06-14"), form.getDateOfOriginalSubmission());
        NonDerivativeTransaction gift = form.getNonDerivativeTable().getNonDerivativeTransaction().getFirst();
        assertEquals(TransactionCode.BONA_FIDE_GIFT, gift.getTransactionCoding().getTransactionCode());
        assertEquals(date("2021-09-29"), gift.getTransactionDate().getValue());
        assertEquals(2000.0, gift.getTransactionAmounts().getTransactionShares().getValue());
    }

    private static List<String> footnoteIds(OwnershipForm form) {
        assertNotNull(form.getFootnotes());
        return form.getFootnotes().getFootnote().stream().map(Footnote::getId).toList();
    }
}
