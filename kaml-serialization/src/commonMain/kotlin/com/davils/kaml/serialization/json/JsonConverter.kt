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
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.boolean
import com.davils.kaml.core.node.double
import com.davils.kaml.core.node.long
import com.davils.kaml.serialization.WalkBudget
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class JsonConverter(limits: YamlLimits) {
    private val budget = WalkBudget.reading(limits)

    fun convert(node: YamlNode): YamlJsonConversion = try {
        YamlJsonConversion.Converted.of(element(node, depth = 0))
    } catch (refused: RefusedNode) {
        refused.conversion
    }

    private fun element(node: YamlNode, depth: Int): JsonElement {
        if (!budget.admits()) refuse(node, "The node expands to more than ${budget.allowance} nodes")
        return when (node) {
            is YamlNode.Scalar -> scalar(node)
            is YamlNode.Sequence -> JsonArray(items(node, depth + 1))
            is YamlNode.Mapping -> JsonObject(members(node, depth + 1))
        }
    }

    private fun items(node: YamlNode.Sequence, depth: Int): List<JsonElement> {
        enter(node, depth)
        return node.items.map { item -> element(item, depth) }
    }

    private fun members(node: YamlNode.Mapping, depth: Int): Map<String, JsonElement> {
        enter(node, depth)
        val members = LinkedHashMap<String, JsonElement>()
        node.entries.forEach { entry ->
            val key = keyOf(entry.key)
            if (key in members) refuse(entry.key, "JSON keys must be unique")
            members[key] = element(entry.value, depth)
        }
        return members
    }

    private fun enter(node: YamlNode, depth: Int) {
        if (budget.isTooDeep(depth)) refuse(node, "The node nests deeper than ${budget.maximumDepth} levels")
    }

    private fun scalar(node: YamlNode.Scalar): JsonElement {
        if (node.tag == YamlTag.NULL) return JsonNull
        return truth(node) ?: number(node) ?: JsonPrimitive(node.value)
    }

    private fun truth(node: YamlNode.Scalar): JsonElement? {
        if (node.tag != YamlTag.BOOLEAN) return null
        return JsonPrimitive(node.boolean() ?: refuse(node, "The boolean has no valid value"))
    }

    private fun number(node: YamlNode.Scalar): JsonElement? {
        if (node.tag == YamlTag.INTEGER) return JsonPrimitive(node.long() ?: refuse(node, INTEGER_REASON))
        return real(node)
    }

    private fun real(node: YamlNode.Scalar): JsonElement? {
        if (node.tag != YamlTag.FLOAT) return null
        val real = node.double()?.takeIf { value -> value.isFinite() }
        return JsonPrimitive(real ?: refuse(node, "JSON has no infinity, no NaN and no float outside a Double"))
    }

    private fun keyOf(key: YamlNode): String {
        val scalar = (key as? YamlNode.Scalar)?.takeIf { scalar -> scalar.tag == YamlTag.STRING }
        return scalar?.value ?: refuse(key, "JSON keys must be strings")
    }

    private fun refuse(node: YamlNode, reason: String): Nothing =
        throw RefusedNode(YamlJsonConversion.Refused.of(node, reason))

    private class RefusedNode(val conversion: YamlJsonConversion.Refused) : RuntimeException(conversion.reason)

    private companion object {
        const val INTEGER_REASON: String = "The integer is outside the range of a Long or has no valid value"
    }
}
