package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.attribute
import com.alexkouzel.sec.http.child
import com.alexkouzel.sec.http.children
import com.alexkouzel.sec.http.hasChildElements
import com.alexkouzel.sec.http.parseDate
import com.alexkouzel.sec.http.text
import com.alexkouzel.sec.http.trimmedText
import org.w3c.dom.Element
import java.math.BigDecimal
import java.time.LocalDate

// The ownership XML's values. A malformed one means the format changed, so it fails.

private const val ISO_DATE_LENGTH = 10

// A value element: <value> and <footnoteId>s, either optional. Some filings put the bare value
// in the element itself. An element with neither value nor footnote, e.g.
// <deemedExecutionDate></deemedExecutionDate>, counts as absent.
internal fun <T> Element?.footnoted(
    name: String,
    convert: (String) -> T,
): Footnoted<T>? {
    val element = this?.child(name) ?: return null
    val text = if (element.hasChildElements()) element.text("value") else element.trimmedText()
    return footnoted(text?.let(convert), element)
}

internal fun <T> footnoted(
    value: T?,
    element: Element,
): Footnoted<T>? {
    val ids = footnoteIds(element)
    if (value == null && ids.isEmpty()) return null
    return Footnoted(value = value, footnoteIds = ids)
}

internal fun footnoteIds(element: Element): List<String> {
    val footnoteIds = element.children("footnoteId")
    return footnoteIds.mapNotNull { it.attribute("id") }
}

internal fun Element.date(name: String): LocalDate? = text(name)?.let(::ownershipDate)

// Some filings add a UTC offset, e.g. 2023-08-16-05:00.
internal fun ownershipDate(text: String): LocalDate = parseDate(text.take(ISO_DATE_LENGTH))

internal fun number(text: String): BigDecimal =
    text.toBigDecimalOrNull()
        ?: throw SecException.Parse("Invalid number in the ownership document: $text")

internal fun Element.flag(name: String): Boolean? {
    val text = text(name) ?: return null
    return when (text.lowercase()) {
        "1", "true" -> true
        "0", "false" -> false
        else -> throw SecException.Parse("Invalid <$name> in the ownership document: $text")
    }
}

internal fun Element.cik(name: String): Long =
    text(name)?.toLongOrNull()
        ?: throw SecException.Parse("Invalid <$name> in the ownership document: ${text(name)}")
