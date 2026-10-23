package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import tools.jackson.core.JacksonException
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.deser.DeserializationProblemHandler
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule

// EDGAR adds fields over time; unknown ones must not break parsing.
internal val json: JsonMapper =
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addHandler(UnknownFields)
        .build()

internal val xml: XmlMapper =
    XmlMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .defaultUseWrapper(false)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addHandler(UnknownFields)
        .build()

internal inline fun <reified T> ObjectMapper.map(content: ByteArray): T =
    try {
        readValue(content, T::class.java)
    } catch (e: JacksonException) {
        throw SecException.Parse("Can't map ${T::class.simpleName}: ${e.originalMessage}", cause = e)
    }

private val unknownFieldSink = ThreadLocal<(String) -> Unit>()

// Reports unknown fields while `block` runs on this thread, e.g. "Issuer.issuerNewThing".
internal fun <T> collectingUnknownFields(
    report: (String) -> Unit,
    block: () -> T,
): T {
    val outer = unknownFieldSink.get()
    unknownFieldSink.set(report)
    try {
        return block()
    } finally {
        unknownFieldSink.set(outer)
    }
}

internal fun unknownField(name: String) {
    unknownFieldSink.get()?.invoke(name)
}

private object UnknownFields : DeserializationProblemHandler() {
    override fun handleUnknownProperty(
        context: DeserializationContext,
        parser: JsonParser,
        deserializer: ValueDeserializer<*>?,
        beanOrClass: Any?,
        propertyName: String,
    ): Boolean {
        val type = beanOrClass as? Class<*> ?: beanOrClass?.javaClass
        unknownField("${type?.name?.substringAfterLast('.')?.replace('$', '.')}.$propertyName")
        parser.skipChildren()
        return true
    }
}
