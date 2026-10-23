package com.alexkouzel.sec.ownership

public data class Footnote(
    val id: String,
    val text: String,
)

/** A value with the IDs of the footnotes attached to it; a footnote may stand in for the value. */
public data class Footnoted<out T>(
    val value: T?,
    val footnoteIds: List<String> = emptyList(),
)
