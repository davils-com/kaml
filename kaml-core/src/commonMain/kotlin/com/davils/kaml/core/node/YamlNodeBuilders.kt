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
package com.davils.kaml.core.node

import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.internal.NodeFactory
import com.davils.kaml.core.node.internal.ScalarValue

private const val TRUE: String = "true"
private const val FALSE: String = "false"

/**
 * Returns a string scalar, tagged [YamlTag.STRING], holding [value].
 *
 * The emitter quotes the value when it cannot be written in [style], such as a plain string that would read as a
 * number or a value with a line break, so any value is written and read back unchanged.
 *
 * @param value the text, which may hold any character.
 * @param style the style the emitter prefers; [YamlScalarStyle.Plain] by default.
 * @return the scalar.
 * @since 1.0.0
 */
public fun yamlScalar(value: String, style: YamlScalarStyle = YamlScalarStyle.Plain): YamlNode.Scalar =
    NodeFactory.scalar(value = value, tag = YamlTag.STRING, style = style, canonical = ScalarValue.Text(value))

/**
 * Returns an integer scalar, tagged [YamlTag.INTEGER], holding [value] in decimal.
 *
 * @param value the integer.
 * @return the scalar.
 * @since 1.0.0
 */
public fun yamlScalar(value: Long): YamlNode.Scalar = NodeFactory.scalar(
    value = value.toString(),
    tag = YamlTag.INTEGER,
    style = YamlScalarStyle.Plain,
    canonical = ScalarValue.Integer(value.toString()),
)

/**
 * Returns an integer scalar, tagged [YamlTag.INTEGER], holding [value] in decimal.
 *
 * @param value the integer.
 * @return the scalar.
 * @since 1.0.0
 */
public fun yamlScalar(value: Int): YamlNode.Scalar = yamlScalar(value.toLong())

/**
 * Returns a float scalar, tagged [YamlTag.FLOAT], holding [value].
 *
 * The text is the shortest decimal that reads back as [value], the same on every target, such as `0.5`, `1.0E21` or
 * `-0.0`; `NaN` and the infinities are written `.nan`, `.inf` and `-.inf`.
 *
 * @param value the number, which may be NaN or infinite.
 * @return the scalar.
 * @since 1.0.0
 */
public fun yamlScalar(value: Double): YamlNode.Scalar = NodeFactory.scalar(
    value = FloatText.of(value),
    tag = YamlTag.FLOAT,
    style = YamlScalarStyle.Plain,
    canonical = ScalarValue.Real(value),
)

/**
 * Returns a boolean scalar, tagged [YamlTag.BOOLEAN], holding `true` or `false`.
 *
 * @param value the boolean.
 * @return the scalar.
 * @since 1.0.0
 */
public fun yamlScalar(value: Boolean): YamlNode.Scalar {
    val text = TRUE.takeIf { value } ?: FALSE
    return NodeFactory.scalar(
        value = text,
        tag = YamlTag.BOOLEAN,
        style = YamlScalarStyle.Plain,
        canonical = ScalarValue.Truth(value),
    )
}

/**
 * Returns a scalar with an explicit [tag], such as a local tag `!ipv4`, holding [value] as its text.
 *
 * The scalar is compared by its tag and its text. kaml never constructs an object from a tag; the tag only names the
 * type a reader may choose.
 *
 * @param value the text, which may hold any character.
 * @param tag the tag of the scalar.
 * @param style the style the emitter prefers; [YamlScalarStyle.Plain] by default.
 * @return the scalar.
 * @since 1.0.0
 */
public fun yamlTagged(value: String, tag: YamlTag, style: YamlScalarStyle = YamlScalarStyle.Plain): YamlNode.Scalar =
    NodeFactory.scalar(
        value = value,
        tag = tag,
        style = style,
        canonical = ScalarValue.Text(value),
    )

/**
 * Returns the null scalar, tagged [YamlTag.NULL] and written `null`.
 *
 * @return the scalar.
 * @since 1.0.0
 */
public fun yamlNull(): YamlNode.Scalar = NodeFactory.scalar(
    value = "null",
    tag = YamlTag.NULL,
    style = YamlScalarStyle.Plain,
    canonical = ScalarValue.Null,
)

/**
 * Returns a sequence holding [items] in order.
 *
 * @param items the items; the sequence keeps a copy.
 * @param style the style the emitter prefers; [YamlCollectionStyle.Block] by default.
 * @param anchor the anchor the emitter writes, or `null` without one.
 * @param tag the tag of the sequence; [YamlTag.SEQUENCE] by default.
 * @return the sequence.
 * @throws IllegalArgumentException if the sequence would nest deeper than
 * [com.davils.kaml.core.limit.YamlLimits.HARD_DEPTH].
 * @since 1.0.0
 */
public fun yamlSequence(
    items: List<YamlNode>,
    style: YamlCollectionStyle = YamlCollectionStyle.Block,
    anchor: YamlAnchor? = null,
    tag: YamlTag = YamlTag.SEQUENCE,
): YamlNode.Sequence = YamlNode.Sequence(
    items = items,
    tag = tag,
    style = style,
    anchor = anchor,
    range = null,
)

/**
 * Returns a block sequence holding the items [build] adds, such as `yamlSequence { add("a"); add(1) }`.
 *
 * @param build the block that adds the items.
 * @return the sequence.
 * @throws IllegalArgumentException if the sequence would nest deeper than
 * [com.davils.kaml.core.limit.YamlLimits.HARD_DEPTH].
 * @since 1.0.0
 */
public fun yamlSequence(build: YamlSequenceBuilder.() -> Unit): YamlNode.Sequence {
    val builder = YamlSequenceBuilder()
    builder.build()
    return yamlSequence(builder.items)
}

/**
 * Returns a mapping holding [entries] in order.
 *
 * @param entries the entries, whose keys must all differ; the mapping keeps a copy.
 * @param style the style the emitter prefers; [YamlCollectionStyle.Block] by default.
 * @param anchor the anchor the emitter writes, or `null` without one.
 * @param tag the tag of the mapping; [YamlTag.MAPPING] by default.
 * @return the mapping.
 * @throws IllegalArgumentException if two of [entries] have equal keys, compared as nodes are compared, or if the
 * mapping would nest deeper than [com.davils.kaml.core.limit.YamlLimits.HARD_DEPTH]; the message names the index of the
 * repeated entry and nothing of its content.
 * @since 1.0.0
 */
public fun yamlMapping(
    entries: List<YamlEntry>,
    style: YamlCollectionStyle = YamlCollectionStyle.Block,
    anchor: YamlAnchor? = null,
    tag: YamlTag = YamlTag.MAPPING,
): YamlNode.Mapping {
    UniqueKeys.requireUnique(entries)
    return YamlNode.Mapping(
        entries = entries,
        tag = tag,
        style = style,
        anchor = anchor,
        range = null,
    )
}

/**
 * Returns a block mapping holding the entries [build] adds, such as `yamlMapping { "port" to 8080 }`.
 *
 * @param build the block that adds the entries.
 * @return the mapping.
 * @throws IllegalArgumentException if [build] adds a key twice, or if the mapping would nest deeper than
 * [com.davils.kaml.core.limit.YamlLimits.HARD_DEPTH].
 * @since 1.0.0
 */
public fun yamlMapping(build: YamlMappingBuilder.() -> Unit): YamlNode.Mapping {
    val builder = YamlMappingBuilder()
    builder.build()
    return yamlMapping(builder.entries)
}
