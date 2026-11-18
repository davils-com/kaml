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

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.serialization.fixture.Blob
import com.davils.kaml.serialization.fixture.Device
import com.davils.kaml.serialization.fixture.Inventory
import com.davils.kaml.serialization.fixture.Keyed
import com.davils.kaml.serialization.fixture.Listener
import com.davils.kaml.serialization.fixture.Optional
import com.davils.kaml.serialization.fixture.Port
import com.davils.kaml.serialization.fixture.Primitives
import com.davils.kaml.serialization.fixture.Protocol
import com.davils.kaml.serialization.fixture.Renamed
import com.davils.kaml.serialization.fixture.Server
import com.davils.kaml.serialization.fixture.Span
import com.davils.kaml.serialization.fixture.Styled
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val PORT: Int = 8080
private const val OTHER_PORT: Int = 53
private const val WEIGHT: Double = 2.5
private const val TIMEOUT: Int = 30
private const val CONNECTIONS: Int = 10
private const val ROUTER_PORTS: Int = 24
private const val VLAN: Int = 10
private const val BYTE: Byte = 7
private const val SHORT: Short = 300
private const val LONG: Long = 5_000_000_000L
private const val FLOAT: Float = 1.5f
private const val BIG_FOR_INT: String = "3000000000"
private const val ROUTE: Int = 1
private const val HOST_LINE: String = "host: web"
private const val PORT_LINE: String = "port: 1"
private const val WEIGHT_VALUE_OFFSET: Int = 18

private val server: Server = Server(
    host = "web",
    listeners = listOf(Listener(Port(PORT)), Listener(Port(OTHER_PORT), Protocol.Udp)),
    labels = mapOf("tier" to "front"),
    weight = WEIGHT,
    owner = null,
)

private fun lines(vararg lines: String): String = lines.joinToString("\n")

class YamlFormatTest : FunSpec({
    val format = YamlFormat.Default

    context("round trips") {
        test("encodes and decodes a server") {
            val text = format.encodeToString(server)

            format.decodeFromString<Server>(text) shouldBe server
        }

        test("writes a comment above an annotated property") {
            format.encodeToString(server) shouldContain "# The host name of the server\nhost: web"
        }

        test("encodes every primitive type") {
            val primitives = Primitives(
                byte = BYTE,
                short = SHORT,
                int = PORT,
                long = LONG,
                float = FLOAT,
                double = WEIGHT,
                char = 'x',
                text = "text",
                isFlag = true,
            )

            format.decodeFromString<Primitives>(format.encodeToString(primitives)) shouldBe primitives
        }

        test("encodes and decodes inline elements") {
            val span = Span(start = 1, end = 2)

            format.encodeToString(span) shouldBe "start: 1\nend: 2\n"
            format.decodeFromString<Span>("start: 1\nend: 2") shouldBe span
        }

        test("encodes binary data as a base64 scalar") {
            val blob = Blob(byteArrayOf(1, 2, 3))
            val text = format.encodeToString(blob)

            text shouldContain "!!binary"
            format.decodeFromString<Blob>(text) shouldBe blob
        }

        test("writes the styles that properties ask for") {
            val text = format.encodeToString(Styled(script = "echo 1\necho 2\n", tags = listOf("a", "b")))

            text shouldContain "script: |"
            text shouldContain "tags: [a, b]"
        }

        test("encodes maps with keys that are not strings") {
            val keyed = Keyed(mapOf(ROUTE to "default"))

            format.decodeFromString<Keyed>(format.encodeToString(keyed)) shouldBe keyed
        }

        test("encodes a value to a node and back") {
            val node = format.encodeToNode(server)

            node["host"].string() shouldBe "web"
            format.decodeFromNode<Server>(node) shouldBe server
        }
    }

    context("decoding problems") {
        test("reports a value of the wrong type at its position") {
            val decoding = format.decodeOrFailure<Server>(lines(HOST_LINE, "weight: heavy"))
            val failed = decoding.shouldBeInstanceOf<YamlDecoding.Failed>()
            val diagnostic = failed.diagnostics.single()

            diagnostic.code shouldBe YamlDiagnosticCode.TypeMismatch
            diagnostic.range.start shouldBe WEIGHT_VALUE_OFFSET
        }

        test("reports a missing property") {
            format.codes<Server>("weight: 2") shouldContain
                YamlDiagnosticCode.MissingProperty
        }

        test("reports an unknown property") {
            format.codes<Server>(lines(HOST_LINE, "colour: red")) shouldContain
                YamlDiagnosticCode.UnknownProperty
        }

        test("reports an unknown enum value") {
            format.codes<Listener>(lines(PORT_LINE, "protocol: icmp")) shouldContain
                YamlDiagnosticCode.UnknownEnumValue
        }

        test("reports a byte outside its range on both sides") {
            format.codes<Primitives>("byte: 300") shouldContain
                YamlDiagnosticCode.NumberOutOfRange
            format.codes<Primitives>("byte: -300") shouldContain
                YamlDiagnosticCode.NumberOutOfRange
        }

        test("reports a number outside its range") {
            format.codes<Listener>("port: $BIG_FOR_INT") shouldContain
                YamlDiagnosticCode.NumberOutOfRange
        }

        test("reports structure where a scalar belongs and the other way round") {
            format.codes<Server>("host: [a]") shouldContain YamlDiagnosticCode.TypeMismatch
            format.codes<Server>(lines("host: a", "listeners: x")) shouldContain
                YamlDiagnosticCode.TypeMismatch
            format.codes<Server>(lines("host: a", "labels: x")) shouldContain
                YamlDiagnosticCode.TypeMismatch
            format.codes<Server>("a") shouldContain YamlDiagnosticCode.TypeMismatch
        }

        test("reports wrong booleans, characters and numbers") {
            val failures = listOf("isFlag: maybe", "char: xy", "double: big", "int: 1.5", "long: x")
            failures.forEach { line ->
                format.codes<Primitives>(line) shouldContain
                    YamlDiagnosticCode.TypeMismatch
            }
        }

        test("reports a syntax error before decoding") {
            format.decodeOrFailure<Server>("host: [").shouldBeInstanceOf<YamlDecoding.Failed>()
        }

        test("throws an exception that names every problem") {
            val failure = shouldThrow<YamlDecodingException> { format.decodeFromString<Server>("weight: heavy") }

            failure.message shouldContain "1:9"
            failure.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.TypeMismatch
        }

        test("refuses a stream of several documents for one value") {
            format.codes<String>(lines("a", "---", "b")) shouldContain
                YamlDiagnosticCode.ExpectedSingleDocument
        }

        test("reports invalid base64 data") {
            format.codes<Blob>("data: !!binary '***'") shouldContain
                YamlDiagnosticCode.TypeMismatch
        }
    }

    context("options") {
        test("ignores unknown keys when asked to") {
            yamlFormat { isUnknownKeyIgnored = true }.decodeFromString<Listener>(lines(PORT_LINE, "extra: 2")) shouldBe
                Listener(Port(1))
        }

        test("omits null values and reads missing ones as null without explicit nulls") {
            val lenient = yamlFormat { hasExplicitNulls = false }

            lenient.encodeToString(Optional()) shouldBe "name: default\nprotocol: udp\n"
            lenient.decodeFromString<Optional>("name: x") shouldBe Optional(name = "x")
        }

        test("leaves out default values when asked to") {
            yamlFormat { isDefaultEncoded = false }.encodeToString(Optional(name = "x")) shouldBe "name: x\n"
        }

        test("coerces invalid values to their defaults when asked to") {
            val coercing = yamlFormat { isInputCoerced = true }

            coercing.decodeFromString<Optional>(lines("name: ~", "protocol: icmp")) shouldBe Optional()
        }

        test("names properties in snake case and kebab case") {
            val snake = yamlFormat { namingStrategy = YamlNamingStrategy.SnakeCase }
            val kebab = yamlFormat { namingStrategy = YamlNamingStrategy.KebabCase }

            snake.encodeToString(Renamed(CONNECTIONS)) shouldContain "max_connections: 10"
            kebab.encodeToString(Renamed(CONNECTIONS)) shouldContain "max-connections: 10"
            kebab.decodeFromString<Renamed>("max-connections: 10") shouldBe Renamed(CONNECTIONS)
        }

        test("reads alternative names") {
            format.decodeFromString<Renamed>(
                lines("maxConnections: 10", "time-out: 30"),
            ) shouldBe Renamed(maxConnections = CONNECTIONS, timeoutSeconds = TIMEOUT)
        }

        test("configures the reader and writer") {
            val compact = yamlFormat { yaml { emitter { indent = 4 } } }

            compact.encodeToString(server) shouldContain "    - port: 8080"
        }

        test("refuses a blank discriminator") {
            shouldThrow<ValidationException> { yamlFormat { classDiscriminator = " " } }
        }

        test("describes itself") {
            format.toString() shouldContain "polymorphism=Property"
        }
    }

    context("polymorphism") {
        val inventory = Inventory(listOf(Device.Router(ROUTER_PORTS), Device.Switch(listOf(VLAN))))

        test("names the type in a property") {
            val text = format.encodeToString(inventory)

            text shouldContain "type: router"
            format.decodeFromString<Inventory>(text) shouldBe inventory
        }

        test("names the type in a tag") {
            val tagged = yamlFormat { polymorphism = YamlPolymorphism.Tag }
            val text = tagged.encodeToString(inventory)

            text shouldContain "!router"
            tagged.decodeFromString<Inventory>(text) shouldBe inventory
        }

        test("wraps the value in its type name") {
            val wrapped = yamlFormat { polymorphism = YamlPolymorphism.Wrapped }
            val text = wrapped.encodeToString(inventory)

            text shouldContain "router:"
            wrapped.decodeFromString<Inventory>(text) shouldBe inventory
        }

        test("reports a missing or unknown type") {
            val tagged = yamlFormat { polymorphism = YamlPolymorphism.Tag }
            val wrapped = yamlFormat { polymorphism = YamlPolymorphism.Wrapped }

            format.codes<Inventory>("devices: [{ports: 1}]") shouldContain
                YamlDiagnosticCode.MissingPolymorphicType
            format.codes<Inventory>("devices: [{type: hub}]") shouldContain
                YamlDiagnosticCode.UnknownPolymorphicType
            format.codes<Inventory>("devices: [a]") shouldContain
                YamlDiagnosticCode.TypeMismatch
            tagged.codes<Inventory>("devices: [{ports: 1}]") shouldContain
                YamlDiagnosticCode.MissingPolymorphicType
            wrapped.codes<Inventory>("devices: [{a: 1, b: 2}]") shouldContain
                YamlDiagnosticCode.MissingPolymorphicType
            wrapped.codes<Inventory>("devices: [{[a]: 1}]") shouldContain
                YamlDiagnosticCode.MissingPolymorphicType
        }
    }

    context("documents") {
        test("decodes every document of a stream") {
            val decoded = format.decodeAll<Listener>(lines(PORT_LINE, "---", "port: 2"))

            decoded.shouldBeInstanceOf<YamlDecoding.Decoded<List<Listener>>>().value shouldBe
                listOf(Listener(Port(1)), Listener(Port(2)))
        }

        test("fails a stream when one document fails") {
            format.decodeAll<Listener>(lines(PORT_LINE, "---", "port: x")).shouldBeInstanceOf<YamlDecoding.Failed>()
            format.decodeAll<Listener>("port: [").shouldBeInstanceOf<YamlDecoding.Failed>()
        }

        test("reads an empty stream as null") {
            format.decodeFromString<String?>("") shouldBe null
        }

        test("reports the problems of a node instead of throwing") {
            val decoding = format.decodeOrFailure<Server>(format.encodeToNode("text"))

            decoding.shouldBeInstanceOf<YamlDecoding.Failed>()
        }

        test("decodes a node without problems") {
            val decoding = format.decodeOrFailure<Server>(format.encodeToNode(server))

            decoding.shouldBeInstanceOf<YamlDecoding.Decoded<Server>>().value shouldBe server
        }

        test("throws for a node it cannot decode") {
            shouldThrow<YamlDecodingException> { format.decodeFromNode<Server>(format.encodeToNode("text")) }
        }
    }
})

private inline fun <reified T> YamlFormat.codes(text: String): List<YamlDiagnosticCode> {
    val decoding = decodeOrFailure<T>(text)
    return decoding.diagnostics.map { it.code }
}
