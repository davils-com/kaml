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

private const val HASH_BASE: Long = 31L
private const val HASH_DIGITS: Int = 7
private const val UNSIGNED_INT: Long = 0xFFFF_FFFFL

internal fun textWithHash(hash: Int): String {
    var remaining = hash.toLong() and UNSIGNED_INT
    val powers = List(HASH_DIGITS) { exponent -> (1..exponent).fold(1L) { power, _ -> power * HASH_BASE } }
    return buildString {
        powers.asReversed().forEach { power ->
            val digit = remaining / power
            append(digit.toInt().toChar())
            remaining -= digit * power
        }
    }
}

internal fun scalarWithHash(hash: Int): YamlNode.Scalar {
    val textHash = hash - HASH_BASE.toInt() * YamlTag.STRING.hashCode()
    return yamlScalar(textWithHash(textHash))
}
