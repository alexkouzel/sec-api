package com.alexkouzel.sec.drift

// The SEC's technical specifications page, which lists the current Ownership spec version.
internal object SpecPage {
    const val URL = "https://www.sec.gov/submit-filings/technical-specifications"

    private const val SECTION = "id=\"ownership_forms\""
    private const val SECTION_REACH = 1_000
    private val sectionVersion = Regex("""Version</strong>(?:\s|&nbsp;)*(\d+(?:\.\d+)*)""")

    // Every spec package link, drafts included (e.g. ...-v5-6_d.zip).
    private val pack = Regex("""ownershipxmltechspec-v(\d+(?:-\d+)*)(_d)?\.zip""")

    // Version in the Ownership section's heading, or null if the page layout changed.
    fun currentVersion(html: String): String? {
        val section = html.indexOf(SECTION).takeIf { it != -1 } ?: return null
        return sectionVersion
            .find(html, section)
            ?.takeIf { it.range.first - section < SECTION_REACH }
            ?.groupValues
            ?.get(1)
    }

    // Versions of every Ownership spec package linked, drafts marked "(draft)".
    fun linkedVersions(html: String): Set<String> =
        pack
            .findAll(html)
            .map { it.groupValues[1].replace('-', '.') + if (it.groupValues[2].isEmpty()) "" else " (draft)" }
            .toSortedSet()

    // Dotted versions compared numerically; a " (draft)" suffix is ignored.
    fun compareVersions(
        a: String,
        b: String,
    ): Int {
        val left = a.removeSuffix(" (draft)").split('.').map(String::toInt)
        val right = b.removeSuffix(" (draft)").split('.').map(String::toInt)
        return (0 until maxOf(left.size, right.size))
            .map { left.getOrElse(it) { 0 }.compareTo(right.getOrElse(it) { 0 }) }
            .firstOrNull { it != 0 } ?: 0
    }
}
