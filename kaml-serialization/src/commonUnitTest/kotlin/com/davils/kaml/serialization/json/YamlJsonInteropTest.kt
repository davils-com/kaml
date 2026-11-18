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

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.node.yamlTagged
import com.davils.kaml.core.yaml
import com.davils.kore.testing.HostileFragments
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

private const val DEPTH: Int = 8
private const val FAN_OUT: Int = 10
private const val LEVELS: Int = 8
private const val ITERATIONS: Int = 200
private const val MOST_ITEMS: Int = 4
private const val FEW: Long = 100
private const val SHORTEST: Int = 3
private const val THOUSANDS: Double = 2500.0
private const val TWENTIETH: Double = -0.05
private const val HUNDRED: Double = 100.0
private const val LONGEST_NUMBER: Long = 123
private const val BIG: String = "123456789012345678901234567890"
private const val BOOLEAN_REASON: String = "The boolean has no valid value"
private const val INTEGER_REASON: String = "The integer is outside the range of a Long or has no valid value"
private const val STRING_KEYS: String = "JSON keys must be strings"
private const val MAYBE: String = "maybe"
private const val REPEATED: String = "{a: 1, a: 2}"
private const val LAST_NONCHARACTER: Int = 0xFFFF

private val shallow: YamlLimits = yamlLimits { maximumDepth = DEPTH }

private val small: YamlLimits = yamlLimits {
    maximumNodes = FEW
    maximumAliasExpansion = FEW
}

private fun refusal(node: YamlNode, limits: YamlLimits = YamlLimits.Untrusted): String {
    val refused = node.toJsonElement(limits).shouldBeInstanceOf<YamlJsonConversion.Refused>()
    return refused.reason
}

private fun converted(node: YamlNode, limits: YamlLimits = YamlLimits.Untrusted): JsonElement {
    val converted = node.toJsonElement(limits).shouldBeInstanceOf<YamlJsonConversion.Converted>()
    return converted.element
}

private fun nestedNode(levels: Int): YamlNode =
    (1 until levels).fold(yamlSequence(emptyList()) as YamlNode) { inner, _ -> yamlSequence(listOf(inner)) }

private fun nestedJson(levels: Int): JsonElement =
    (1 until levels).fold(JsonArray(emptyList()) as JsonElement) { inner, _ -> JsonArray(listOf(inner)) }

private fun pair(key: YamlNode, value: YamlNode): YamlNode = yamlMapping(listOf(YamlEntry(key = key, value = value)))

private val finiteDoubles: Arb<Double> = Arb.double().filter { number -> number.isFinite() }

private val leaves: Arb<JsonElement> = Arb.choice(
    Arb.string(0..MOST_ITEMS).map { text -> JsonPrimitive(text) },
    Arb.long().map { number -> JsonPrimitive(number) },
    finiteDoubles.map { number -> JsonPrimitive(number) },
    Arb.boolean().map { flag -> JsonPrimitive(flag) },
    Arb.constant(JsonNull),
)

private val elements: Arb<JsonElement> = arbitrary {
    val items = Arb.list(leaves, 0..MOST_ITEMS).bind()
    val keys = Arb.list(Arb.string(0..MOST_ITEMS), items.size..items.size).bind()
    JsonObject(mapOf("list" to JsonArray(items), "object" to JsonObject(keys.zip(items).toMap())))
}

@OptIn(ExperimentalSerializationApi::class)
class YamlJsonInteropTest : FunSpec({
    context("keys") {
        test("refuses keys that are no strings") {
            refusal(pair(key = yamlScalar(1), value = yamlScalar("a"))) shouldBe STRING_KEYS
            refusal(pair(key = yamlScalar(true), value = yamlScalar("a"))) shouldBe STRING_KEYS
        }

        test("converts only the occurrence of a repeated key that the reader keeps") {
            val keepFirst = yaml { duplicateKeys = YamlDuplicateKeys.KeepFirst }
            val parsed = keepFirst.parse(REPEATED).shouldBeInstanceOf<YamlParseResult.Parsed>()
            val documents = parsed.stream.documents

            converted(documents.single().root) shouldBe JsonObject(mapOf("a" to JsonPrimitive(1)))
            Yaml.Default.parse(REPEATED).shouldBeInstanceOf<YamlParseResult.Failed>()
        }

        test("cannot meet two keys with the same text, since a mapping refuses them") {
            val entries = listOf(
                YamlEntry(key = yamlScalar("a"), value = yamlScalar(1)),
                YamlEntry(key = yamlTagged("a", YamlTag.STRING), value = yamlScalar(2)),
            )

            shouldThrow<IllegalArgumentException> { yamlMapping(entries) }
        }
    }

    context("scalars") {
        test("refuses an integer outside the range of a Long") {
            val parsed = Yaml.Default.parse(BIG).shouldBeInstanceOf<YamlParseResult.Parsed>()
            val documents = parsed.stream.documents

            refusal(documents.single().root) shouldBe INTEGER_REASON
        }

        test("refuses scalars whose text is no value of their tag") {
            refusal(yamlTagged(MAYBE, YamlTag.BOOLEAN)) shouldBe BOOLEAN_REASON
            refusal(
                yamlTagged("x", YamlTag.FLOAT),
            ) shouldBe "JSON has no infinity, no NaN and no float outside a Double"
            refusal(yamlTagged("x", YamlTag.INTEGER)) shouldBe INTEGER_REASON
        }
    }

    context("limits") {
        test("refuses a node nested as deep as the limit") {
            converted(nestedNode(DEPTH - 1), shallow) shouldBe nestedJson(DEPTH - 1)
            refusal(nestedNode(DEPTH), shallow) shouldBe "The node nests deeper than $DEPTH levels"
            refusal(yamlMapping { "a" to nestedNode(DEPTH - 1) }, shallow) shouldBe
                "The node nests deeper than $DEPTH levels"
        }

        test("refuses a graph whose shared nodes expand beyond the budget") {
            val shared = (1..LEVELS).fold(yamlScalar(1) as YamlNode) { inner, _ ->
                yamlSequence(List(FAN_OUT) { inner })
            }

            refusal(shared, small) shouldBe "The node expands to more than 200 nodes"
        }

        test("refuses an element nested as deep as the limit") {
            nestedJson(DEPTH - 1).toYamlNode(shallow) shouldBe nestedNode(DEPTH - 1)
            shouldThrow<SerializationException> { nestedJson(DEPTH).toYamlNode(shallow) }.message shouldBe
                "The element nests deeper than $DEPTH levels"
            shouldThrow<SerializationException> {
                JsonObject(mapOf("a" to nestedJson(DEPTH - 1))).toYamlNode(shallow)
            }
        }

        test("refuses an element whose shared parts expand beyond the budget") {
            val shared = (1..LEVELS).fold(JsonPrimitive(1) as JsonElement) { inner, _ ->
                JsonArray(List(FAN_OUT) { inner })
            }

            shouldThrow<SerializationException> { shared.toYamlNode(small) }.message shouldBe
                "The element expands to more than 200 elements"
        }
    }

    context("JSON literals") {
        test("convert integers, reals and keywords") {
            JsonUnquotedLiteral("-0").toYamlNode() shouldBe yamlScalar(0)
            JsonUnquotedLiteral("2.5e3").toYamlNode() shouldBe yamlScalar(THOUSANDS)
            JsonUnquotedLiteral("-0.5E-1").toYamlNode() shouldBe yamlScalar(TWENTIETH)
            JsonUnquotedLiteral("1E+2").toYamlNode() shouldBe yamlScalar(HUNDRED)
            JsonUnquotedLiteral("true").toYamlNode() shouldBe yamlScalar(true)
        }

        test("refuse what JSON or a Long or Double cannot hold, without echoing it") {
            val refused =
                listOf(BIG, "1e400", "abc", "01", "1.", ".5", "1e", "-", "+1", "NaN", "Infinity", "0x10", "1_0")
            refused.forEach { literal ->
                val failure = shouldThrow<SerializationException> { JsonUnquotedLiteral(literal).toYamlNode() }

                failure.message.orEmpty() shouldNotContain literal
            }
        }

        test("refuse a number longer than allowed") {
            val short = yamlLimits { maximumNumberLength = SHORTEST }

            JsonUnquotedLiteral("$LONGEST_NUMBER").toYamlNode(short) shouldBe yamlScalar(LONGEST_NUMBER)
            shouldThrow<SerializationException> { JsonUnquotedLiteral("1234").toYamlNode(short) }.message shouldBe
                "A JSON number is longer than allowed"
        }

        test("refuse strings that YAML cannot represent") {
            shouldThrow<SerializationException> {
                JsonPrimitive(
                    "a${HostileFragments.LONE_HIGH_SURROGATE}",
                ).toYamlNode()
            }
            shouldThrow<SerializationException> {
                JsonObject(
                    mapOf(Char(LAST_NONCHARACTER).toString() to JsonNull),
                ).toYamlNode()
            }
        }
    }

    context("conversions") {
        test("compare by content") {
            val converted = yamlScalar(1).toJsonElement()
            val same = yamlScalar(1).toJsonElement()

            converted shouldBe same
            converted.hashCode() shouldBe same.hashCode()
            converted shouldNotBe yamlScalar(2).toJsonElement()
            converted.toString() shouldBe "Converted"
            converted shouldNotBe yamlTagged(MAYBE, YamlTag.BOOLEAN).toJsonElement()
        }

        test("compare refusals by node and reason") {
            val refused = yamlTagged(MAYBE, YamlTag.BOOLEAN).toJsonElement()
            val same = yamlTagged(MAYBE, YamlTag.BOOLEAN).toJsonElement()

            refused shouldBe same
            refused.hashCode() shouldBe same.hashCode()
            refused shouldNotBe yamlTagged("perhaps", YamlTag.BOOLEAN).toJsonElement()
            refused shouldNotBe yamlTagged("x", YamlTag.FLOAT).toJsonElement()
            refused shouldNotBe yamlScalar(1).toJsonElement()
            refused.toString() shouldBe "Refused(reason=$BOOLEAN_REASON)"
        }

        test("round trip JSON through YAML") {
            checkAll(ITERATIONS, elements) { element ->
                converted(element.toYamlNode()) shouldBe element
            }
        }
    }
})
