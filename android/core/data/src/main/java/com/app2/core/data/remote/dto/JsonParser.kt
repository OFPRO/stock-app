package com.app2.core.data.remote.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@PublishedApi internal val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

inline fun <reified T> JsonElement.deserialize(): T = json.decodeFromString(this.toString())

fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

fun JsonElement?.optInt(): Int? = this?.jsonPrimitive?.intOrNull

fun JsonElement?.optDouble(): Double? = this?.jsonPrimitive?.doubleOrNull

fun JsonElement?.optBoolean(): Boolean? = this?.jsonPrimitive?.let {
    if (it is JsonNull) null else it.content.toBooleanStrictOrNull()
}

fun JsonElement.asList(): List<JsonElement> = this.jsonArray.toList()

fun JsonElement.asObject(): Map<String, JsonElement> = this.jsonObject

fun JsonElement.mapToList(transform: (Map<String, JsonElement>) -> JsonElement?): List<JsonElement> =
    jsonArray.mapNotNull { element ->
        val obj = element.jsonObject
        transform(obj)
    }
