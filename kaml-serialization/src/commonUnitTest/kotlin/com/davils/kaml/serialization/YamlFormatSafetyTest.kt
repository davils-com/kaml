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
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.node.yamlTagged
import com.davils.kaml.serialization.fixture.Fatal
import com.davils.kaml.serialization.fixture.Guarded
import com.davils.kaml.serialization.fixture.Inventory
import com.davils.kaml.serialization.fixture.Listener
import com.davils.kaml.serialization.fixture.Missing
import com.davils.kaml.serialization.fixture.Primitives
import com.davils.kaml.serialization.fixture.Server
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

private const val ITERATIONS: Int = 200
private const val MARKER: String = "secret-input"
private const val OTHER_TAG: String = "!other"
private const val GUARDED: String = "name: $MARKER"
private const val NONCHARACTER_CODE: Int = 0xFFFE
private const val LAST_NONCHARACTER_CODE: Int = 0xFFFF

private val NONCHARACTER: String = Char(NONCHARACTER_CODE).toString()
private val LAST_NONCHARACTER: String = Char(LAST_NONCHARACTER_CODE).toString()

private val secrets: Arb<String> = Arb.string(8..24, Codepoint.alphanumeric()).map { text -> MARKER + text }

private val hostile: Arb<String> = Arb.list(Arb.element(HostileFragments.all), 1..64).map { it.joinToString("") }

private val unsafe: Arb<String> = hostile.filter { text -> !isSafeToShow(text, false) }

private val representable: Arb<String> = hostile.filter { text -> ScalarText.isRepresentable(text) }

private fun lines(vararg lines: String): String = lines.joinToString("\n")

private fun unknownEntry(name: String): YamlEntry = YamlEntry(key = yamlScalar(name), value = yamlScalar(1))

private fun YamlDecoding<*>.messages(): List<String> = diagnostics.map { diagnostic -> diagnostic.message }

private fun YamlDecoding<*>.shouldBeSafeToShow() {
    messages().forEach { message -> isSafeToShow(message, false) shouldBe true }
}

@OptIn(DavilsInternal::class)
class YamlFormatSafetyTest : FunSpec({
    val format = YamlFormat.Default
    val quoting = yamlFormat { isValueQuoted = true }

    context("names and values from the document") {
        test("escape an unknown property name") {
            checkAll(ITERATIONS, unsafe) { name ->
                val node = yamlMapping(
                    listOf(
                        YamlEntry(key = yamlScalar("port"), value = yamlScalar(1)),
                        YamlEntry(key = yamlScalar(name), value = yamlScalar(2)),
                    ),
                )
                val decoding = format.decodeOrFailure<Listener>(node)

                decoding.diagnostics.map { it.code } shouldBe listOf(YamlDiagnosticCode.UnknownProperty)
                decoding.shouldBeSafeToShow()
                decoding.messages().single() shouldNotContain name
            }
        }

        test("escape an unknown enum value") {
            checkAll(ITERATIONS, unsafe) { value ->
                val decoding = quoting.decodeOrFailure<Listener>(
                    yamlMapping {
                        "port" to 1
                        "protocol" to value
                    },
                )

                decoding.diagnostics.map { it.code } shouldBe listOf(YamlDiagnosticCode.UnknownEnumValue)
                decoding.shouldBeSafeToShow()
            }
        }

        test("escape an unknown type name") {
            checkAll(ITERATIONS, unsafe) { name ->
                val device = yamlMapping { "type" to name }
                val decoding = format.decodeOrFailure<Inventory>(
                    yamlMapping { "devices" to yamlSequence(listOf(device)) },
                )

                decoding.diagnostics.map { it.code } shouldBe listOf(YamlDiagnosticCode.UnknownPolymorphicType)
                decoding.shouldBeSafeToShow()
            }
        }

        test("escape a value of the wrong type and two keys that decode to the same map key") {
            checkAll(ITERATIONS, unsafe) { value ->
                val labels = listOf(
                    YamlEntry(key = yamlScalar(value), value = yamlScalar(1)),
                    YamlEntry(key = yamlTagged(value, YamlTag(OTHER_TAG)), value = yamlScalar(2)),
                )
                val node = yamlMapping {
                    "host" to "a"
                    "weight" to value
                    "labels" to yamlMapping(labels)
                }
                val decoding = quoting.decodeOrFailure<Server>(node)

                decoding.diagnostics.map { it.code } shouldBe
                    listOf(YamlDiagnosticCode.DuplicateKey, YamlDiagnosticCode.TypeMismatch)
                decoding.shouldBeSafeToShow()
            }
        }

        test("escape the name of a missing field that a serializer reports") {
            checkAll(ITERATIONS, unsafe) { field ->
                val decoding = format.decodeOrFailure<Missing>(yamlScalar(field))

                decoding.diagnostics.map { it.code } shouldBe listOf(YamlDiagnosticCode.MissingProperty)
                decoding.shouldBeSafeToShow()
            }
        }

        test("escape every line of the exception") {
            checkAll(ITERATIONS, representable) { name ->
                val node = yamlMapping(
                    listOf(YamlEntry(key = yamlScalar("host"), value = yamlScalar("a")), unknownEntry(name)),
                )
                val text = Yaml.Default.emit(node)
                val failure = shouldThrow<YamlDecodingException> { format.decodeFromString<Server>(text) }
                val lines = failure.message.orEmpty().lines()

                lines.forEach { line -> isSafeToShow(line, false) shouldBe true }
            }
        }
    }

    context("values from the document") {
        test("are left out of every message unless the format quotes them") {
            checkAll(ITERATIONS, secrets) { secret ->
                val text = lines("host: a", "weight: $secret", "labels: {$secret: 1, !other $secret: 2}")
                val enum = lines("port: 1", "protocol: $secret")
                val decodings = listOf(format.decodeOrFailure<Server>(text), format.decodeOrFailure<Listener>(enum))
                val failure = shouldThrow<YamlDecodingException> { format.decodeFromString<Server>(text) }

                decodings.flatMap { decoding -> decoding.messages() }.forEach { message ->
                    message shouldNotContain secret
                }
                failure.message.orEmpty() shouldNotContain secret
                val quoted = quoting.decodeOrFailure<Server>(text)
                quoted.messages().forEach { message -> message shouldContain secret }
            }
        }

        test("are shown as the redacted placeholder") {
            val decoding = format.decodeOrFailure<Listener>(lines("port: hunter2", "protocol: hunter3"))

            decoding.messages() shouldBe listOf("Expected an integer, but found ${SafeText.REDACTED}")
            val enum = format.decodeOrFailure<Listener>("{port: 1, protocol: hunter3}")
            enum.messages().single() shouldBe "${SafeText.REDACTED} is none of tcp, udp"
        }
    }

    context("exceptions of serializers") {
        test("replace the message of a require in an init block by a redacted placeholder") {
            val decoding = format.decodeOrFailure<Guarded>(GUARDED)

            decoding.diagnostics.map { it.code } shouldBe listOf(YamlDiagnosticCode.TypeMismatch)
            decoding.messages().single() shouldBe "The value is refused: ${SafeText.REDACTED}"
            val failure = shouldThrow<YamlDecodingException> { format.decodeFromString<Guarded>(GUARDED) }

            failure.message.orEmpty() shouldNotContain MARKER
        }

        test("keep no cause that could carry the input") {
            shouldThrow<YamlDecodingException> { format.decodeFromString<Guarded>(GUARDED) }.cause shouldBe null
        }

        test("never catch an error") {
            shouldThrow<AssertionError> { format.decodeOrFailure<Fatal>("error") }
        }

        test("pass on an exception that no contract names") {
            shouldThrow<IllegalStateException> { format.decodeOrFailure<Fatal>("state") }
            format.decodeFromString<Fatal>("fine") shouldBe Fatal("fine")
        }
    }

    context("strings that YAML cannot represent") {
        test("are refused when encoded") {
            listOf(
                "a${HostileFragments.LONE_HIGH_SURROGATE}",
                HostileFragments.LONE_LOW_SURROGATE,
                NONCHARACTER,
                "x$LAST_NONCHARACTER",
            ).forEach { text ->
                shouldThrow<SerializationException> {
                    format.encodeToString(Server.serializer(), Server(host = text))
                }.message shouldBe
                    "A string holds a lone surrogate or the noncharacter U+FFFE or U+FFFF, which YAML cannot represent"
            }
        }

        test("are refused as keys and characters") {
            shouldThrow<SerializationException> {
                format.encodeToString(
                    Server.serializer(),
                    Server(host = "a", labels = mapOf(HostileFragments.LONE_HIGH_SURROGATE to "x")),
                )
            }
            shouldThrow<SerializationException> {
                format.encodeToNode(
                    Primitives.serializer(),
                    Primitives(
                        byte = 0,
                        short = 0,
                        int = 0,
                        long = 0,
                        float = 0f,
                        double = 0.0,
                        char = HostileFragments.LONE_LOW_SURROGATE.single(),
                        text = "",
                        isFlag = false,
                    ),
                )
            }
        }

        test("keep a surrogate pair") {
            val server = Server(host = "😀")

            format.decodeFromString<Server>(format.encodeToString(Server.serializer(), server)) shouldBe server
        }
    }
})
