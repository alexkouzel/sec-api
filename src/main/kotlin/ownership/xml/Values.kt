package com.alexkouzel.sec.ownership.xml

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.ownership.Footnoted
import java.time.LocalDate
import java.time.format.DateTimeParseException

// Converts the XML's text values. A malformed one means the format changed, so it fails.

private const val ISO_DATE_LENGTH = 10

// An element with neither a value nor a footnote, e.g. <deemedExecutionDate></deemedExecutionDate>,
// counts as absent.
internal fun <T> Value?.footnoted(convert: (String) -> T): Footnoted<T>? {
    if (this == null) return null
    val text = value.text()
    return footnoted(text?.let(convert), footnoteId)
}

internal fun <T> footnoted(
    value: T?,
    footnoteIds: List<FootnoteId>,
): Footnoted<T>? {
    if (value == null && footnoteIds.isEmpty()) return null
    return Footnoted(value, footnoteIds.ids())
}

internal fun List<FootnoteId>.ids(): List<String> = mapNotNull { it.id.text() }

internal fun String?.text(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

internal fun String?.date(): LocalDate? = text()?.let(::parseDate)

// Some filings add a UTC offset, e.g. 2023-08-16-05:00.
internal fun parseDate(text: String): LocalDate {
    try {
        return LocalDate.parse(text.take(ISO_DATE_LENGTH))
    } catch (e: DateTimeParseException) {
        throw SecException.Parse("Invalid date in the ownership document: $text", e)
    }
}

internal fun number(text: String): Double =
    text.toDoubleOrNull()
        ?: throw SecException.Parse("Invalid number in the ownership document: $text")

internal fun String?.flag(): Boolean? =
    when (text()?.lowercase()) {
        null -> null
        "1", "true" -> true
        "0", "false" -> false
        else -> throw SecException.Parse("Invalid flag in the ownership document: $this")
    }

internal fun String?.cik(element: String): Long =
    text()?.toLongOrNull()
        ?: throw SecException.Parse("Invalid <$element> in the ownership document: $this")
