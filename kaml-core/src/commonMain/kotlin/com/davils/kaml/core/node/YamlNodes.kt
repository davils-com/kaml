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

import com.davils.kaml.core.model.YamlTag

/**
 * Returns the value of the first entry of this mapping whose key is a scalar with the text [key], whatever the tag
 * of that key, so `["1"]` finds both the key `"1"` and the key `1`.
 *
 * It is meant for reading a document step by step, such as `node["server"]["port"].int()`: every step on a node that
 * is no mapping, or on `null`, returns `null`. [YamlNode.Mapping.get] finds a key by its node instead.
 *
 * @param key the text of the key to look for.
 * @return the value, or `null` if this is no mapping or has no such key.
 * @since 1.0.0
 */
public operator fun YamlNode?.get(key: String): YamlNode? {
    val mapping = this as? YamlNode.Mapping ?: return null
    val entry = mapping.entries.firstOrNull { candidate -> (candidate.key as? YamlNode.Scalar)?.value == key }
    return entry?.value
}

/**
 * Returns the item of this sequence at [index].
 *
 * @param index the position of the item, from zero.
 * @return the item, or `null` if this is no sequence or has no item at [index].
 * @since 1.0.0
 */
public operator fun YamlNode?.get(index: Int): YamlNode? {
    val sequence = this as? YamlNode.Sequence ?: return null
    return sequence.items.getOrNull(index)
}

/**
 * Whether this is `null` or a scalar tagged [YamlTag.NULL], such as `null`, `~` or an empty value.
 *
 * @since 1.0.0
 */
public val YamlNode?.isNull: Boolean
    get() {
        val isNullScalar = this is YamlNode.Scalar && tag == YamlTag.NULL
        return this == null || isNullScalar
    }

/**
 * Returns the text of this scalar, whatever its tag, such as `"8080"` for the integer `8080`.
 *
 * @return the text, or `null` if this is no scalar or [isNull].
 * @since 1.0.0
 */
public fun YamlNode?.string(): String? {
    val scalar = this as? YamlNode.Scalar ?: return null
    return scalar.value.takeIf { !scalar.isNull }
}

/**
 * Returns the value of this integer scalar as a [Long].
 *
 * @return the value, or `null` if this is no integer scalar or its value lies outside the range of [Long]; it never
 * wraps around.
 * @since 1.0.0
 */
public fun YamlNode?.long(): Long? {
    val integer = canonicalOf(this) as? ScalarValue.Integer ?: return null
    return integer.decimal.toLongOrNull()
}

/**
 * Returns the value of this integer scalar as an [Int].
 *
 * @return the value, or `null` if this is no integer scalar or its value lies outside the range of [Int]; it never
 * wraps around.
 * @since 1.0.0
 */
public fun YamlNode?.int(): Int? {
    val value = long() ?: return null
    return value.toInt().takeIf { value in Int.MIN_VALUE..Int.MAX_VALUE }
}

/**
 * Returns the value of this float or integer scalar as a finite [Double], rounded to the nearest double the same way
 * on every target.
 *
 * @return the value, or `null` if this is no float or integer scalar, or if its value is not finite: `.nan`, `.inf`,
 * `-.inf` and an integer too large for a double give `null`.
 * @since 1.0.0
 */
public fun YamlNode?.double(): Double? {
    val canonical = canonicalOf(this)
    val integer = canonical as? ScalarValue.Integer
    val value = (canonical as? ScalarValue.Real)?.value ?: integer?.let { NumberTexts.finiteOrNull(it.decimal) }
    return value?.takeIf(Double::isFinite)
}

/**
 * Returns the value of this boolean scalar.
 *
 * @return the value, or `null` if this is no boolean scalar.
 * @since 1.0.0
 */
public fun YamlNode?.boolean(): Boolean? {
    val truth = canonicalOf(this) as? ScalarValue.Truth
    return truth?.isTrue
}

private fun canonicalOf(node: YamlNode?): ScalarValue? = (node as? YamlNode.Scalar)?.canonical
