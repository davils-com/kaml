/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kaml.core.conformance.fixture

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.boolean
import com.davils.kaml.core.node.double
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

internal object JsonView {
    private val numericTags: Set<YamlTag> = setOf(YamlTag.INTEGER, YamlTag.FLOAT)

    fun expected(text: String): List<JsonElement> = JsonValues.split(text).map { value ->
        normalized(Json.parseToJsonElement(value))
    }

    fun of(node: YamlNode): JsonElement? = when (node) {
        is YamlNode.Scalar -> scalar(node)
        is YamlNode.Sequence -> sequence(node)
        is YamlNode.Mapping -> mapping(node)
    }

    private fun sequence(node: YamlNode.Sequence): JsonElement? {
        val items = node.items.map(::of)
        if (items.any { it == null }) return null
        return JsonArray(items.filterNotNull())
    }

    private fun mapping(node: YamlNode.Mapping): JsonElement? {
        val pairs = node.entries.map { entry -> (entry.key as? YamlNode.Scalar)?.value to of(entry.value) }
        if (pairs.any { (key, value) -> key == null || value == null }) return null
        return JsonObject(pairs.associate { (key, value) -> key.orEmpty() to (value ?: JsonNull) })
    }

    private fun scalar(node: YamlNode.Scalar): JsonElement {
        if (node.tag == YamlTag.NULL) return JsonNull
        val primitive = JsonPrimitive(node.boolean()).takeIf { node.tag == YamlTag.BOOLEAN }
        return primitive ?: JsonPrimitive(node.double()).takeIf { node.tag in numericTags } ?: JsonPrimitive(node.value)
    }

    private fun normalized(element: JsonElement): JsonElement = when (element) {
        is JsonArray -> JsonArray(element.map(::normalized))
        is JsonObject -> JsonObject(element.mapValues { (_, value) -> normalized(value) })
        is JsonPrimitive -> number(element) ?: element
    }

    private fun number(primitive: JsonPrimitive): JsonPrimitive? {
        val value = primitive.doubleOrNull?.takeIf { !primitive.isString }
        return value?.let(::JsonPrimitive)
    }
}
