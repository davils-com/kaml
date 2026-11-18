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

package com.davils.kaml.core.schema

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.IntegerText
import com.davils.kaml.core.node.ScalarValue

private const val OCTAL_RADIX: Int = 8
private const val HEX_RADIX: Int = 16
private const val OCTAL_PREFIX: String = "0o"
private const val HEX_PREFIX: String = "0x"

internal object CoreForms : ScalarForms {
    private val nulls: Set<String> = setOf("", "~", "null", "Null", "NULL")
    private val booleans: Map<String, Boolean> = mapOf(
        "true" to true,
        "True" to true,
        "TRUE" to true,
        "false" to false,
        "False" to false,
        "FALSE" to false,
    )

    private val recognizers: List<Pair<YamlTag, (String) -> Boolean>> = listOf(
        YamlTag.NULL to { text -> text in nulls },
        YamlTag.BOOLEAN to { text -> text in booleans },
        YamlTag.INTEGER to ::isInteger,
        YamlTag.FLOAT to NumberForms::isCoreFloat,
    )

    private val readers: Map<YamlTag, (String) -> ScalarValue?> = mapOf(
        YamlTag.NULL to { text -> ScalarValue.Null.takeIf { text in nulls } },
        YamlTag.BOOLEAN to { text -> booleans[text]?.let(ScalarValue::Truth) },
        YamlTag.INTEGER to ::integer,
        YamlTag.FLOAT to ::real,
        YamlTag.STRING to ScalarValue::Text,
    )

    override fun resolve(text: String): YamlTag {
        val recognized = recognizers.firstOrNull { (_, recognizes) -> recognizes(text) }
        return recognized?.first ?: YamlTag.STRING
    }

    override fun valueOf(tag: YamlTag, text: String): ScalarValue? = readers[tag]?.invoke(text)

    private fun isInteger(text: String): Boolean {
        val isRadix = NumberForms.isRadix(text, OCTAL_PREFIX, TextScan::isOctalDigit) ||
            NumberForms.isRadix(text, HEX_PREFIX, TextScan::isHexDigit)
        return isRadix || NumberForms.isCoreDecimal(text)
    }

    private fun integer(text: String): ScalarValue? {
        val decimal = radixValue(text) ?: decimalValue(text)
        return decimal?.let(ScalarValue::Integer)
    }

    private fun decimalValue(text: String): String? {
        if (!NumberForms.isCoreDecimal(text)) return null
        val isNegative = text.startsWith("-")
        return IntegerText.decimal(isNegative, text.trimStart('-', '+'))
    }

    private fun radixValue(text: String): String? {
        val isOctal = NumberForms.isRadix(text, OCTAL_PREFIX, TextScan::isOctalDigit)
        val isHex = NumberForms.isRadix(text, HEX_PREFIX, TextScan::isHexDigit)
        val radix = OCTAL_RADIX.takeIf { isOctal } ?: HEX_RADIX.takeIf { isHex } ?: return null
        return IntegerText.ofRadix(isNegative = false, digits = text.drop(OCTAL_PREFIX.length), radix = radix)
    }

    private fun real(text: String): ScalarValue? {
        val special = NumberForms.specialFloat(text)
        if (special != null) return ScalarValue.Real(special)
        val value = text.takeIf(NumberForms::isCoreFloat)?.let(NumberForms::finite)
        return value?.let(ScalarValue::Real)
    }
}
