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

package com.davils.kaml.serialization

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.serialization.fixture.Device
import com.davils.kaml.serialization.fixture.Inventory
import com.davils.kaml.serialization.fixture.Listener
import com.davils.kaml.serialization.fixture.Primitives
import com.davils.kaml.serialization.fixture.Protocol
import com.davils.kaml.serialization.fixture.Sample
import com.davils.kaml.serialization.fixture.Server
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.char
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.codepoints
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.short
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val ITERATIONS: Int = 250
private const val LONGEST: Int = 24
private const val MOST_ITEMS: Int = 4
private const val MOST_TOKENS: Int = 48

private val representable: Arb<Codepoint> = Arb.codepoints().filter { codepoint ->
    codepoint.value !in 0xD800..0xDFFF && codepoint.value != 0xFFFE && codepoint.value != 0xFFFF
}

private val texts: Arb<String> = Arb.choice(
    Arb.string(0..LONGEST, representable),
    Arb.element("", "~", "null", "true", "no", "1", "-.inf", "0x1F", "- a", "a: b", "#", "---", "...", " lead", "end "),
)

private val hostile: Arb<String> = Arb.list(Arb.element(HostileFragments.all), 0..64).map { it.joinToString("") }

private val characters: Arb<Char> = Arb.char(Char.MIN_VALUE..Char.MAX_VALUE).filter { character ->
    ScalarText.isRepresentable(character.toString())
}

private val devices: Arb<Device> = Arb.choice(
    Arb.int().map { ports -> Device.Router(ports) },
    Arb.list(Arb.int(), 0..MOST_ITEMS).map { vlans -> Device.Switch(vlans) },
)

private val labels: Arb<Map<String, String>> = Arb.list(texts, 0..MOST_ITEMS).map { keys ->
    keys.associateWith { key -> key.reversed() }
}

private val samples: Arb<Sample> = arbitrary {
    Sample(
        text = texts.bind(),
        letter = characters.bind(),
        tiny = Arb.byte().bind(),
        short = Arb.short().bind(),
        number = Arb.int().bind(),
        big = Arb.long().bind(),
        small = Arb.float().bind(),
        ratio = Arb.double().bind(),
        isOn = Arb.boolean().bind(),
        note = texts.orNull().bind(),
        tags = Arb.list(texts, 0..MOST_ITEMS).bind(),
        labels = labels.bind(),
        protocol = Arb.enum<Protocol>().bind(),
        device = devices.bind(),
    )
}

private val primitiveSamples: Arb<Primitives> = arbitrary {
    primitives(
        long = Arb.long().bind(),
        float = Arb.float().bind(),
        double = Arb.double().bind(),
        character = characters.bind(),
    )
}

private val formats: List<YamlFormat> = listOf(
    YamlFormat.Default,
    yamlFormat {
        polymorphism = YamlPolymorphism.Tag
        namingStrategy = YamlNamingStrategy.SnakeCase
    },
    yamlFormat {
        polymorphism = YamlPolymorphism.Wrapped
        namingStrategy = YamlNamingStrategy.KebabCase
        hasExplicitNulls = false
    },
)

private val tokens: Arb<String> = Arb.choice(
    Arb.element(
        "{", "}", "[", "]", ":", ": ", ", ", " ", "  ", "\n", "\n  ", "- ", "? ", "&a ", "*a", "!x ", "!!binary ",
        "!!int ", "'", "\"", "#", "|", ">", "<<: ", "---\n", "...\n", "%YAML 1.2\n", "\t", "\\", "type", "host",
        "port", "devices", "router", "switch", "ports", "vlans", "labels", "weight", "1", "-1", "1e400", ".inf", ".nan",
        "0x7F", "9223372036854775808", "~", "true", "\r",
    ),
    Arb.element(HostileFragments.all),
)

private val documents: Arb<String> = Arb.list(tokens, 0..MOST_TOKENS).map { it.joinToString("") }

class YamlFormatPropertyTest : FunSpec({
    context("round trips") {
        test("decode what they encode to text") {
            checkAll(ITERATIONS, samples) { sample ->
                formats.forEach { format ->
                    format.decodeFromString<Sample>(format.encodeToString(sample)) shouldBe sample
                }
            }
        }

        test("decode what they encode to a node") {
            checkAll(ITERATIONS, samples) { sample ->
                formats.forEach { format -> format.decodeFromNode<Sample>(format.encodeToNode(sample)) shouldBe sample }
            }
        }

        test("keep hostile strings as values and keys or refuse them as unrepresentable") {
            checkAll(ITERATIONS, hostile, hostile) { value, key ->
                val server = Server(host = value, labels = mapOf(key to value), owner = key)
                val isRepresentable = ScalarText.isRepresentable(value) && ScalarText.isRepresentable(key)
                if (isRepresentable) {
                    YamlFormat.Default.decodeFromString<Server>(
                        YamlFormat.Default.encodeToString(server),
                    ) shouldBe server
                }
                if (!isRepresentable) {
                    shouldThrow<SerializationException> { YamlFormat.Default.encodeToString(server) }
                }
            }
        }

        test("keep every primitive") {
            checkAll(ITERATIONS, primitiveSamples) { primitives ->
                val text = YamlFormat.Default.encodeToString(primitives)

                YamlFormat.Default.decodeFromString<Primitives>(text) shouldBe primitives
            }
        }
    }

    context("random YAML") {
        test("never throws an unexpected exception") {
            checkAll(ITERATIONS, documents) { text ->
                formats.forEach { format ->
                    format.decodeOrFailure<Server>(text)
                    format.decodeOrFailure<Inventory>(text)
                    format.decodeAll<Listener>(text)
                    format.decodeOrFailure<Map<String, List<Int?>>>(text)
                }
            }
        }

        test("only throws a decoding exception with lines that are safe to show") {
            checkAll(ITERATIONS, documents) { text ->
                val failure = runCatchingDecoding { YamlFormat.Default.decodeFromString<Server>(text) }
                val lines = failure?.message.orEmpty().lines()

                lines.forEach { line -> isSafeToShow(line, false) shouldBe true }
            }
        }

        test("only reports decoding problems that are safe to show") {
            checkAll(ITERATIONS, documents) { text ->
                val parsed = Yaml.Default.parse(text) as? YamlParseResult.Parsed
                val roots = parsed?.run { stream.documents }.orEmpty()
                roots.forEach { document ->
                    YamlFormat.Default.decodeOrFailure<Inventory>(document.root).diagnostics.forEach { diagnostic ->
                        isSafeToShow(diagnostic.message, false) shouldBe true
                    }
                }
            }
        }
    }
})

private fun primitives(long: Long, float: Float, double: Double, character: Char): Primitives = Primitives(
    byte = 1,
    short = 2,
    int = 3,
    long = long,
    float = float,
    double = double,
    char = character,
    text = "",
    isFlag = true,
)

private fun runCatchingDecoding(block: () -> Unit): YamlDecodingException? = try {
    block()
    null
} catch (failure: YamlDecodingException) {
    failure
}
