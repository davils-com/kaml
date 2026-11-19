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
package com.davils.kaml.core.compose

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlUnknownTags
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.fixture.visibleTagOf
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.schema.yamlSchema
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.yaml
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val ITERATIONS: Int = 200
private const val MOST_FRAGMENTS: Int = 64
private const val HEX_RADIX: Int = 16
private const val BYTE_MASK: Int = 0xFF
private const val UNICODE_DIGITS: Int = 4
private const val SEPARATORS: String = " \t\r\n,[]{}"

private val hostile: Arb<String> =
    Arb.list(Arb.element(HostileFragments.all), 0..MOST_FRAGMENTS).map { fragments -> fragments.joinToString("") }

class HostileTextTest : FunSpec({
    val strict = yaml { unknownTags = YamlUnknownTags.Refuse }
    val json = yaml { schema = YamlSchema.Json }

    context("diagnostics") {
        test("show no input unescaped") {
            checkAll(ITERATIONS, hostile) { text ->
                val name = "n" +
                    text
                        .map { character -> 'x'.takeIf { character in SEPARATORS } ?: character }
                        .joinToString("")
                val quoted = "\"${escaped(text)}\""
                val documents = listOf(
                    Yaml.Default to text("$quoted: 1", "$quoted: 2"),
                    Yaml.Default to "a: *$name",
                    Yaml.Default to "&$name [*$name]",
                    Yaml.Default to text("%$name x", "---", "a"),
                    Yaml.Default to "!!int $quoted",
                    Yaml.Default to """"\$text"""",
                    Yaml.Default to text,
                    json to "- $name",
                    strict to "!t${percentEncoded(text)} x",
                )
                documents.forEach { (reader, document) ->
                    reader.parse(document).diagnostics.forEach { diagnostic ->
                        unsafeCodePointsIn(diagnostic.message).shouldBeEmpty()
                    }
                }
            }
        }

        test("show no tag of a schema unescaped") {
            checkAll(ITERATIONS, hostile) { text ->
                val tag = visibleTagOf(text)
                val custom = yamlSchema {
                    name = text
                    mapping(tag.uri)
                }
                val reader = yaml {
                    schema = custom
                    unknownTags = YamlUnknownTags.Refuse
                }
                val shown = listOf(custom.toString(), custom.tags.single().toString())
                val messages = reader.parse("!${percentEncoded(tag.uri.drop(1))} x").diagnostics.map { it.message }

                (shown + messages).forEach { message -> unsafeCodePointsIn(message).shouldBeEmpty() }
            }
        }
    }

    context("sources") {
        test("show no name unescaped") {
            checkAll(ITERATIONS, hostile) { text ->
                unsafeCodePointsIn(YamlSource.of("a", name = text).toString()).shouldBeEmpty()
            }
        }
    }
})

private fun escaped(text: String): String =
    text.map { character -> "\\u" + character.code.toString(HEX_RADIX).padStart(UNICODE_DIGITS, '0') }.joinToString("")

private fun percentEncoded(text: String): String = text.encodeToByteArray().joinToString("") { byte ->
    "%" + (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
}
