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

package com.davils.kaml.serialization.json

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.serialization.ScalarText
import com.davils.kaml.serialization.WalkBudget
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.parseDecimalOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(DavilsInternal::class)
internal class YamlNodeConverter(private val limits: YamlLimits) {
    private val budget = WalkBudget.reading(limits)

    fun convert(element: JsonElement): YamlNode = node(element, depth = 0)

    private fun node(element: JsonElement, depth: Int): YamlNode {
        if (!budget.admits()) refuse("The element expands to more than ${budget.allowance} elements")
        return when (element) {
            is JsonPrimitive -> primitive(element)
            is JsonArray -> yamlSequence(items(element, depth + 1))
            is JsonObject -> yamlMapping(entries(element, depth + 1))
        }
    }

    private fun items(array: JsonArray, depth: Int): List<YamlNode> {
        enter(depth)
        return array.map { item -> node(item, depth) }
    }

    private fun entries(json: JsonObject, depth: Int): List<YamlEntry> {
        enter(depth)
        return json.map { member -> YamlEntry(key = text(member.key), value = node(member.value, depth)) }
    }

    private fun enter(depth: Int) {
        if (budget.isTooDeep(depth)) refuse("The element nests deeper than ${budget.maximumDepth} levels")
    }

    private fun primitive(primitive: JsonPrimitive): YamlNode {
        if (primitive.isString) return text(primitive.content)
        return literal(primitive.content)
    }

    private fun literal(content: String): YamlNode {
        val keyword = keywords[content]
        if (keyword != null) return keyword()
        return number(content)
    }

    private fun number(content: String): YamlNode {
        if (content.length > limits.maximumNumberLength) refuse("A JSON number is longer than allowed")
        val shape = JsonNumberShape.of(content)
        if (shape == JsonNumberShape.Integer) return yamlScalar(content.toLongOrNull() ?: refuse(INTEGER_REASON))
        val real = parseDecimalOrNull(content).takeIf { shape == JsonNumberShape.Real }
        return yamlScalar(real ?: refuse("The literal is no JSON value or is outside the range of a Double"))
    }

    private fun text(value: String): YamlNode {
        if (!ScalarText.isRepresentable(value)) refuse(UNREPRESENTABLE_REASON)
        return yamlScalar(value)
    }

    private fun refuse(reason: String): Nothing = throw SerializationException(reason)

    private companion object {
        const val INTEGER_REASON: String = "A JSON integer is outside the range of a Long"
        const val UNREPRESENTABLE_REASON: String =
            "A JSON string holds a lone surrogate or the noncharacter U+FFFE or U+FFFF, which YAML cannot represent"

        val keywords: Map<String, () -> YamlNode> = mapOf(
            "null" to { yamlNull() },
            "true" to { yamlScalar(true) },
            "false" to { yamlScalar(false) },
        )
    }
}
