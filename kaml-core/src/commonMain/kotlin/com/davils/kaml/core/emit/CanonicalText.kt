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

package com.davils.kaml.core.emit

import com.davils.kaml.core.node.FloatText
import com.davils.kaml.core.node.ScalarValue
import com.davils.kaml.core.node.YamlNode

internal object CanonicalText {
    private const val EXPONENT: String = "E"
    private const val NEGATIVE_EXPONENT: String = "E-"
    private const val POSITIVE_EXPONENT: String = "E+"

    fun of(node: YamlNode.Scalar): String? = when (val canonical = node.canonical) {
        is ScalarValue.Truth -> canonical.isTrue.toString()
        is ScalarValue.Integer -> canonical.decimal
        is ScalarValue.Real -> realText(canonical.value)
        is ScalarValue.Null -> "null"
        is ScalarValue.Text -> null
    }

    private fun realText(value: Double): String {
        val text = FloatText.of(value)
        val isPositiveExponent = EXPONENT in text && NEGATIVE_EXPONENT !in text
        return text.replace(oldValue = EXPONENT, newValue = POSITIVE_EXPONENT).takeIf { isPositiveExponent } ?: text
    }
}
