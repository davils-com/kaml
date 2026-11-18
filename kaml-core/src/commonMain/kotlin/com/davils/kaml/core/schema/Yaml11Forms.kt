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

private const val BINARY_RADIX: Int = 2
private const val OCTAL_RADIX: Int = 8
private const val HEX_RADIX: Int = 16
private const val RADIX_PREFIX_LENGTH: Int = 2
private const val BASE64_WHITESPACE: String = " \t\r\n"

internal object Yaml11Forms : ScalarForms {
    private val nulls: Set<String> = setOf("", "~", "null", "Null", "NULL")
    private val truths: Set<String> = setOf("y", "Y", "yes", "Yes", "YES", "true", "True", "TRUE", "on", "On", "ON")
    private val falsities: Set<String> =
        setOf("n", "N", "no", "No", "NO", "false", "False", "FALSE", "off", "Off", "OFF")

    private val recognizers: List<Pair<YamlTag, (String) -> Boolean>> = listOf(
        YamlTag.NULL to { text -> text in nulls },
        YamlTag.BOOLEAN to { text -> text in truths || text in falsities },
        YamlTag.INTEGER to ::isInteger,
        YamlTag.FLOAT to ::isFloat,
        YamlTag.MERGE to { text -> text == "<<" },
        YamlTag.VALUE to { text -> text == "=" },
        YamlTag.TIMESTAMP to ::isTimestamp,
    )

    private val readers: Map<YamlTag, (String) -> ScalarValue?> = mapOf(
        YamlTag.NULL to { text -> ScalarValue.Null.takeIf { text in nulls } },
        YamlTag.BOOLEAN to ::truth,
        YamlTag.INTEGER to ::integer,
        YamlTag.FLOAT to ::floating,
        YamlTag.MERGE to { text -> ScalarValue.Text(text).takeIf { text == "<<" } },
        YamlTag.VALUE to ScalarValue::Text,
        YamlTag.TIMESTAMP to { text -> ScalarValue.Text(text).takeIf { isTimestamp(text) } },
        YamlTag.BINARY to { text -> ScalarValue.Text(text.filterNot { character -> character in BASE64_WHITESPACE }) },
        YamlTag.STRING to ScalarValue::Text,
    )

    override fun resolve(text: String): YamlTag {
        val recognized = recognizers.firstOrNull { (_, recognizes) -> recognizes(text) }
        return recognized?.first ?: YamlTag.STRING
    }

    override fun valueOf(tag: YamlTag, text: String): ScalarValue? = readers[tag]?.invoke(text)

    private fun isInteger(text: String): Boolean = Yaml11Patterns.isBinary(text) ||
        Yaml11Patterns.isHex(text) ||
        Yaml11Patterns.isOctal(text) ||
        Yaml11Patterns.isDecimal(text) ||
        Yaml11Patterns.isSexagesimal(text)

    private fun isFloat(text: String): Boolean = NumberForms.specialFloat(text) != null ||
        Yaml11Patterns.isSexagesimalReal(text) ||
        Yaml11Patterns.isReal(text)

    private fun isTimestamp(text: String): Boolean = Yaml11Patterns.isDate(text) || Yaml11Patterns.isTimestamp(text)

    private fun truth(text: String): ScalarValue? {
        if (text in truths) return ScalarValue.Truth(true)
        return ScalarValue.Truth(false).takeIf { text in falsities }
    }

    private fun integer(text: String): ScalarValue? {
        val isNegative = text.startsWith("-")
        val compact = text.trimStart('-', '+').replace(oldValue = "_", newValue = "")
        val decimal = radix(text, compact) ?: decimalOrSexagesimal(text, isNegative, compact)
        return decimal?.let(ScalarValue::Integer)
    }

    private fun radix(text: String, compact: String): String? {
        val isNegative = text.startsWith("-")
        val binary = BINARY_RADIX.takeIf { Yaml11Patterns.isBinary(text) }
        val prefixed = binary ?: HEX_RADIX.takeIf { Yaml11Patterns.isHex(text) }
        val radix = prefixed ?: OCTAL_RADIX.takeIf { Yaml11Patterns.isOctal(text) } ?: return null
        val prefixLength = RADIX_PREFIX_LENGTH.takeIf { prefixed != null } ?: 1
        return IntegerText.ofRadix(isNegative, compact.drop(prefixLength), radix)
    }

    private fun decimalOrSexagesimal(text: String, isNegative: Boolean, compact: String): String? {
        if (Yaml11Patterns.isDecimal(text)) return IntegerText.decimal(isNegative, compact)
        return compact.takeIf { Yaml11Patterns.isSexagesimal(text) }?.let { digits -> sexagesimal(isNegative, digits) }
    }

    private fun sexagesimal(isNegative: Boolean, compact: String): String {
        val places = compact.split(':')
        return IntegerText.ofSexagesimal(isNegative, places.first(), places.drop(1))
    }

    private fun floating(text: String): ScalarValue? {
        val special = NumberForms.specialFloat(text)
        if (special != null) return ScalarValue.Real(special)
        val value = sexagesimalReal(text) ?: real(text)
        return value?.let(ScalarValue::Real)
    }

    private fun sexagesimalReal(text: String): Double? {
        if (!Yaml11Patterns.isSexagesimalReal(text)) return null
        val sign = "-".takeIf { text.startsWith("-") }.orEmpty()
        val compact = text.trimStart('-', '+').replace(oldValue = "_", newValue = "")
        val places = compact.substringBefore('.').split(':')
        val whole = IntegerText.ofSexagesimal(isNegative = false, leading = places.first(), places = places.drop(1))
        return NumberForms.finite("$sign$whole.${compact.substringAfter('.')}")
    }

    private fun real(text: String): Double? {
        if (!Yaml11Patterns.isReal(text)) return null
        return NumberForms.finite(text.replace(oldValue = "_", newValue = ""))
    }
}
