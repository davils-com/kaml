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
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlTagged
import com.davils.kaml.serialization.fixture.Animal
import com.davils.kaml.serialization.fixture.Cat
import com.davils.kaml.serialization.fixture.Device
import com.davils.kaml.serialization.fixture.Drawing
import com.davils.kaml.serialization.fixture.Inventory
import com.davils.kaml.serialization.fixture.ManualRouter
import com.davils.kaml.serialization.fixture.REFUSED
import com.davils.kaml.serialization.fixture.Shape
import com.davils.kaml.serialization.fixture.Zoo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private const val LIVES: Int = 9
private const val PORTS: Int = 24
private const val ROUTER: String = "router"

private val animals: SerializersModule = SerializersModule {
    polymorphic(Animal::class) {
        subclass(Cat::class, Cat.serializer())
    }
}

private val styles: List<YamlPolymorphism> = YamlPolymorphism.entries

private fun codes(decoding: YamlDecoding<*>): List<YamlDiagnosticCode> = decoding.diagnostics.map { it.code }

class YamlFormatPolymorphismTest : FunSpec({
    context("the closed registry") {
        test("refuses the class name of a subtype in place of its serial name") {
            val name = "com.davils.kaml.serialization.fixture.Device.Router"
            val decoding = YamlFormat.Default.decodeOrFailure<Inventory>("devices: [{type: $name, ports: 1}]")

            codes(decoding) shouldBe listOf(YamlDiagnosticCode.UnknownPolymorphicType)
            decoding.diagnostics.single().message shouldBe "There is no type \"$name\""
        }

        test("refuses a class of the platform") {
            codes(YamlFormat.Default.decodeOrFailure<Inventory>("devices: [{type: kotlin.String}]")) shouldBe
                listOf(YamlDiagnosticCode.UnknownPolymorphicType)
        }

        test("refuses an open subtype that the module does not register") {
            val decoding = YamlFormat.Default.decodeOrFailure<Zoo>("animals: [{type: cat, lives: 9}]")

            codes(decoding) shouldBe listOf(YamlDiagnosticCode.UnknownPolymorphicType)
            shouldThrow<SerializationException> { YamlFormat.Default.encodeToString(Zoo(listOf(Cat(LIVES)))) }
        }

        test("decodes an open subtype that the module registers") {
            val zoo = Zoo(listOf(Cat(LIVES)))
            styles.forEach { style ->
                val format = yamlFormat {
                    serializersModule = animals
                    polymorphism = style
                }

                format.decodeFromString<Zoo>(format.encodeToString(zoo)) shouldBe zoo
            }
        }

        test("reports an unknown type in every style") {
            val tagged = yamlFormat { polymorphism = YamlPolymorphism.Tag }
            val wrapped = yamlFormat { polymorphism = YamlPolymorphism.Wrapped }

            codes(tagged.decodeOrFailure<Inventory>("devices: [!hub {ports: 1}]")) shouldBe
                listOf(YamlDiagnosticCode.UnknownPolymorphicType)
            codes(wrapped.decodeOrFailure<Inventory>("devices: [{hub: {ports: 1}}]")) shouldBe
                listOf(YamlDiagnosticCode.UnknownPolymorphicType)
        }
    }

    context("the structure protocol") {
        test("encodes a serializer of polymorphic kind that is no built-in one like a sealed type") {
            val manual = ManualRouter(ROUTER, Device.Router(PORTS))

            YamlFormat.Default.encodeToString(manual) shouldBe "type: router\nports: 24\n"
            styles.forEach { style ->
                val format = yamlFormat { polymorphism = style }

                format.decodeFromString<ManualRouter>(format.encodeToString(manual)) shouldBe manual
            }
        }

        test("hands the type name to a serializer of polymorphic kind as its first element") {
            YamlFormat.Default.decodeFromString<ManualRouter>("{type: anything, ports: 1}") shouldBe
                ManualRouter("anything", Device.Router(1))
        }

        test("reports a refusal after the value was read as a refused value, not as an unknown type") {
            val decoding = YamlFormat.Default.decodeOrFailure<ManualRouter>("{type: $REFUSED, ports: 1}")

            codes(decoding) shouldBe listOf(YamlDiagnosticCode.TypeMismatch)
            decoding.diagnostics.single().message shouldNotContain REFUSED
        }

        test("reads the type from a quoted discriminator key") {
            YamlFormat.Default.decodeFromString<Device>("{\"type\": router, ports: 2}") shouldBe Device.Router(2)
        }

        test("reads the type of a tagged node built in code") {
            val tagged = yamlFormat { polymorphism = YamlPolymorphism.Tag }
            val node = yamlTagged("2", YamlTag("!circle"))

            tagged.decodeFromNode<Shape>(node) shouldBe Shape.Circle("2")
        }

        test("round trips a scalar subtype wrapped in its type name") {
            val format = yamlFormat { polymorphism = YamlPolymorphism.Wrapped }
            val text = format.encodeToString(Drawing(listOf(Shape.Circle("1"))))

            text.lines() shouldBe listOf("shapes:", """  - circle: "1"""", "")
            format.decodeFromString<Drawing>(text) shouldBe Drawing(listOf(Shape.Circle("1")))
        }
    }

    context("missing types") {
        test("reports a node that cannot carry a type in every style") {
            val tagged = yamlFormat { polymorphism = YamlPolymorphism.Tag }
            val wrapped = yamlFormat { polymorphism = YamlPolymorphism.Wrapped }

            codes(tagged.decodeOrFailure<Device>(yamlScalar(ROUTER))) shouldBe
                listOf(YamlDiagnosticCode.MissingPolymorphicType)
            codes(wrapped.decodeOrFailure<Device>(yamlScalar(ROUTER))) shouldBe
                listOf(YamlDiagnosticCode.MissingPolymorphicType)
            codes(
                YamlFormat.Default.decodeOrFailure<Device>(yamlMapping { "type" to yamlMapping(emptyList()) }),
            ) shouldBe
                listOf(YamlDiagnosticCode.MissingPolymorphicType)
        }

        test("names the discriminator it expected") {
            YamlFormat.Default
                .decodeOrFailure<Device>("{ports: 1}")
                .diagnostics
                .single()
                .message shouldContain
                "Expected the property \"type\" to choose the type"
        }
    }
})
