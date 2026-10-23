package com.alexkouzel.sec.ownership.xml

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.ownership.Footnoted
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val ISO_DATE_LENGTH = 10

// An element with neither a value nor a footnote, e.g. <deemedExecutionDate></deemedExecutionDate>,
// is absent.
internal fun <T> Value?.footnoted(convert: (String) -> T): Footnoted<T>? =
    this?.let { footnoted(value.text()?.let(convert), footnoteId) }

internal fun <T> footnoted(
    value: T?,
    footnoteIds: List<FootnoteId>,
): Footnoted<T>? = if (value == null && footnoteIds.isEmpty()) null else Footnoted(value, footnoteIds.ids())

internal fun List<FootnoteId>.ids() = mapNotNull { it.id.text() }

internal fun String?.text(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

internal fun String?.date(): LocalDate? = text()?.let(::parseDate)

// Some filings add a UTC offset, e.g. 2023-08-16-05:00.
internal fun parseDate(text: String): LocalDate =
    try {
        LocalDate.parse(text.take(ISO_DATE_LENGTH))
    } catch (e: DateTimeParseException) {
        throw SecException.Parse("Invalid date in the ownership document: $text", cause = e)
    }

internal fun number(text: String): Double =
    text.toDoubleOrNull() ?: throw SecException.Parse("Invalid number in the ownership document: $text")

internal fun String?.flag(): Boolean? =
    when (text()?.lowercase()) {
        null -> null
        "1", "true" -> true
        "0", "false" -> false
        else -> throw SecException.Parse("Invalid flag in the ownership document: $this")
    }

internal fun String?.cik(element: String): Long =
    text()?.toLongOrNull() ?: throw SecException.Parse("Invalid <$element> in the ownership document: $this")
