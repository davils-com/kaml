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
import com.davils.kaml.serialization.fixture.Acronyms
import com.davils.kaml.serialization.fixture.Drawing
import com.davils.kaml.serialization.fixture.Holder
import com.davils.kaml.serialization.fixture.Inventory
import com.davils.kaml.serialization.fixture.Listener
import com.davils.kaml.serialization.fixture.Nullable
import com.davils.kaml.serialization.fixture.Optional
import com.davils.kaml.serialization.fixture.PickyHolder
import com.davils.kaml.serialization.fixture.Server
import com.davils.kaml.serialization.fixture.Shape
import com.davils.kaml.serialization.fixture.Upper
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val PORT: Int = 80
private const val TTL: Int = 60

private fun codes(decoding: YamlDecoding<*>): List<YamlDiagnosticCode> = decoding.diagnostics.map { it.code }

@OptIn(DavilsInternal::class)
class YamlFormatEdgeTest : FunSpec({
    val format = YamlFormat.Default
    val lenient = yamlFormat { hasExplicitNulls = false }

    context("nulls") {
        test("reads a missing nullable property as null without explicit nulls") {
            lenient.decodeFromString<Nullable>("{}") shouldBe Nullable(null)
        }

        test("keeps nulls inside a list without explicit nulls") {
            lenient.encodeToString(Holder(listOf("a", null))) shouldContain "- null"
        }

        test("refuses null for a string") {
            codes(format.decodeOrFailure<Server>("host: ~")) shouldContain YamlDiagnosticCode.TypeMismatch
        }
    }

    context("keys") {
        test("reports a key that is no scalar") {
            codes(
                format.decodeOrFailure<Listener>("{[a]: 1, port: 1}"),
            ) shouldContain YamlDiagnosticCode.UnknownProperty
        }

        test("coerces an enum written as a collection") {
            val coercing = yamlFormat { isInputCoerced = true }

            coercing.decodeFromString<Optional>("protocol: [x]") shouldBe Optional()
        }

        test("splits names with several capitals as one word") {
            val snake = yamlFormat { namingStrategy = YamlNamingStrategy.SnakeCase }

            snake.encodeToString(Acronyms(httpPort = PORT, dnsTTL = TTL)) shouldContain "dns_ttl: 60"
        }
    }

    context("serializers that refuse values") {
        test("replace the message of the serializer by a redacted placeholder") {
            val decoding = format.decodeOrFailure<PickyHolder>("value: loud")
            val failed = decoding.shouldBeInstanceOf<YamlDecoding.Failed>()

            failed.diagnostics.single().message shouldBe "The value is refused: ${SafeText.REDACTED}"
        }

        test("report a refusal without a message") {
            codes(
                format.decodeOrFailure<PickyHolder>("value: silent"),
            ) shouldContain YamlDiagnosticCode.TypeMismatch
        }
    }

    context("polymorphism") {
        test("reports a type that is no scalar") {
            codes(format.decodeOrFailure<Inventory>("devices: [{type: [x]}]")) shouldContain
                YamlDiagnosticCode.MissingPolymorphicType
            codes(format.decodeOrFailure<Inventory>("devices: [{[type]: x}]")) shouldContain
                YamlDiagnosticCode.MissingPolymorphicType
        }

        test("reports a wrapped value that is no mapping") {
            val wrapped = yamlFormat { polymorphism = YamlPolymorphism.Wrapped }

            codes(
                wrapped.decodeOrFailure<Inventory>("devices: [a]"),
            ) shouldContain YamlDiagnosticCode.MissingPolymorphicType
        }
    }

    context("scalar subtypes") {
        test("keep a scalar subtype as it is") {
            val drawing = Drawing(listOf(Shape.Circle("2")))

            format.encodeToString(drawing) shouldContain "- \"2\""
            yamlFormat { polymorphism = YamlPolymorphism.Tag }.encodeToString(drawing) shouldContain "- \"2\""
        }

        test("name a property that starts with a capital") {
            yamlFormat { namingStrategy = YamlNamingStrategy.SnakeCase }.encodeToString(
                Upper("x"),
            ) shouldBe "url: x\n"
        }

        test("write a nullable property that has a value") {
            lenient.encodeToString(Nullable("x")) shouldBe "note: x\n"
            format.encodeToString(Nullable(null)) shouldBe "note: null\n"
        }
    }

    context("warnings") {
        test("keep a decoded value") {
            val decoded = format.decodeOrFailure<Listener>("%YAML 1.3\n--- {port: 1}")

            decoded.shouldBeInstanceOf<YamlDecoding.Decoded<Listener>>().diagnostics.map { it.code } shouldContain
                YamlDiagnosticCode.NewerMinorVersion
        }
    }
})
