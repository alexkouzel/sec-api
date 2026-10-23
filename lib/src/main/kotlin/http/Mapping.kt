package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule

// EDGAR adds fields over time; unknown ones must not break parsing.
internal val json: JsonMapper =
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

internal val xml: XmlMapper =
    XmlMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .defaultUseWrapper(false)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

internal inline fun <reified T> ObjectMapper.map(content: ByteArray): T =
    try {
        readValue(content, T::class.java)
    } catch (e: JacksonException) {
        throw SecException.Parse("Can't map ${T::class.simpleName}: ${e.originalMessage}", e)
    }
