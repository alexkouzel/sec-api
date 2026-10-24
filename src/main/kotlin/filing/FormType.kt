package com.alexkouzel.sec.filing

/** The kind of filing, as EDGAR names it: `4`, `4/A`, `10-K`, `424B2`, ... */
public data class FormType(
    val value: String,
) {
    public val isAmendment: Boolean get() = value.endsWith("/A")

    override fun toString(): String = value

    public companion object {
        @JvmField public val F3: FormType = FormType("3")

        @JvmField public val F3_A: FormType = FormType("3/A")

        @JvmField public val F4: FormType = FormType("4")

        @JvmField public val F4_A: FormType = FormType("4/A")

        @JvmField public val F5: FormType = FormType("5")

        @JvmField public val F5_A: FormType = FormType("5/A")

        @JvmField public val F8_K: FormType = FormType("8-K")

        @JvmField public val F10_K: FormType = FormType("10-K")

        @JvmField public val F10_Q: FormType = FormType("10-Q")
    }
}
