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
import com.davils.kaml.core.node.YamlNode
import kotlinx.serialization.json.JsonElement

/**
 * Converts this node to a [JsonElement] that holds the same data.
 *
 * A null becomes [kotlinx.serialization.json.JsonNull], a boolean, an integer within the range of a `Long` and a
 * finite float become JSON literals, and every other scalar, whatever its tag, becomes a JSON string of its text. The
 * conversion is refused, with a [YamlJsonConversion.Refused] that names the node, for what JSON cannot hold exactly:
 * a key that is not a string, two keys with the same text, an integer outside the range of a `Long`, an infinite
 * or not-a-number float, and a boolean, integer or float scalar whose text is no such value. It is also refused for a
 * node nested deeper than [YamlLimits.maximumDepth], and for a graph that expands to more than
 * [YamlLimits.maximumNodes] plus [YamlLimits.maximumAliasExpansion] nodes, counting a node shared by several aliases
 * once per reference.
 *
 * @param limits the limits of the conversion; [YamlLimits.Untrusted] by default.
 * @return the converted element, or why the node cannot be converted.
 * @since 1.0.0
 */
public fun YamlNode.toJsonElement(limits: YamlLimits = YamlLimits.Untrusted): YamlJsonConversion =
    JsonConverter(limits).convert(this)

/**
 * Converts this JSON element to a [YamlNode] that holds the same data.
 *
 * [kotlinx.serialization.json.JsonNull] becomes a null, a JSON string a string scalar, `true` and `false` booleans, a
 * JSON integer an integer and any other JSON number a float; an array becomes a sequence and an object a mapping with
 * string keys.
 *
 * @param limits the limits of the conversion; [YamlLimits.Untrusted] by default.
 * @return the converted node, without positions.
 * @throws kotlinx.serialization.SerializationException if the element nests deeper than [YamlLimits.maximumDepth],
 * expands to more than [YamlLimits.maximumNodes] plus [YamlLimits.maximumAliasExpansion] elements, counting an
 * element shared in several places once per reference, holds a literal that is no JSON value, an integer outside the
 * range of a `Long`, a number longer than [YamlLimits.maximumNumberLength] or outside the range of a `Double`, or a
 * string with a lone surrogate or the noncharacter U+FFFE or U+FFFF, which YAML cannot represent. The message never
 * contains the input.
 * @since 1.0.0
 */
public fun JsonElement.toYamlNode(limits: YamlLimits = YamlLimits.Untrusted): YamlNode =
    YamlNodeConverter(limits).convert(this)
