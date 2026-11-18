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
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.serialization.decode.DecodeContext
import com.davils.kaml.serialization.fixture.AliasClash
import com.davils.kaml.serialization.fixture.Clash
import com.davils.kaml.serialization.fixture.Labelled
import com.davils.kaml.serialization.fixture.Listener
import com.davils.kaml.serialization.fixture.Nest
import com.davils.kaml.serialization.fixture.Nullable
import com.davils.kaml.serialization.fixture.Renamed
import com.davils.kaml.serialization.fixture.SelfAlias
import com.davils.kaml.serialization.fixture.Server
import com.davils.kaml.serialization.fixture.SnakeClash
import com.davils.kaml.serialization.fixture.Tree
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val DEPTH: Int = 8
private const val COPIES: Int = 3
private const val AMOUNT: Int = 3
private const val FAN_OUT: Int = 10
private const val LEVELS: Int = 7
private const val FEW: Int = 50
private const val DIAGNOSTICS: Int = 3
private const val UNKNOWN_KEYS: Int = 6

private val shallow: YamlFormat = yamlFormat { yaml { limits { maximumDepth = DEPTH } } }

private val small: YamlFormat = yamlFormat {
    yaml {
        limits {
            maximumNodes = FEW.toLong()
            maximumAliasExpansion = FEW.toLong()
        }
    }
}

private fun nested(levels: Int): YamlNode =
    (1 until levels).fold(yamlMapping(emptyList()) as YamlNode) { inner, _ -> yamlMapping { "next" to inner } }

private fun shared(levels: Int): YamlNode = (1..levels).fold(yamlMapping(emptyList()) as YamlNode) { inner, _ ->
    yamlMapping { "children" to yamlSequence(List(FAN_OUT) { inner }) }
}

private fun codes(decoding: YamlDecoding<*>): List<YamlDiagnosticCode> = decoding.diagnostics.map { it.code }

@OptIn(DavilsInternal::class)
class YamlFormatLimitsTest : FunSpec({
    context("depth") {
        test("decodes a node graph nested one level less than the limit") {
            shallow.decodeFromNode<Nest>(nested(DEPTH - 1)) shouldBe Nest.of(DEPTH - 1)
        }

        test("refuses a node graph handed in that nests as deep as the limit") {
            codes(shallow.decodeOrFailure<Nest>(nested(DEPTH))) shouldContainExactly listOf(YamlDiagnosticCode.TooDeep)
        }

        test("refuses a deep node graph when it throws") {
            val failure = shouldThrow<YamlDecodingException> { shallow.decodeFromNode<Nest>(nested(DEPTH)) }

            failure.message shouldContain "nests deeper than $DEPTH levels"
        }

        test("decodes text nested as deep as the reader allows") {
            val text = shallow.encodeToString(Nest.of(DEPTH - 1))

            shallow.decodeFromString<Nest>(text) shouldBe Nest.of(DEPTH - 1)
        }

        test("refuses to encode a value the reader could not read back") {
            shouldThrow<SerializationException> { shallow.encodeToString(Nest.of(DEPTH)) }.message shouldBe
                "The value nests deeper than $DEPTH levels"
            shouldThrow<SerializationException> { shallow.encodeToNode(Nest.of(DEPTH)) }
        }

        test("encodes and decodes a value as deep as the hard limit allows without exhausting the stack") {
            val deep = yamlFormat { yaml { limits = YamlLimits.Relaxed } }
            val deepest = Nest.of(YamlLimits.HARD_DEPTH - 1)

            deep.decodeFromString<Nest>(deep.encodeToString(deepest)) shouldBe deepest
            deep.decodeFromNode<Nest>(deep.encodeToNode(deepest)) shouldBe deepest
            codes(deep.decodeOrFailure<Nest>(nested(YamlLimits.HARD_DEPTH))) shouldBe
                listOf(YamlDiagnosticCode.TooDeep)
            shouldThrow<SerializationException> { deep.encodeToNode(Nest.of(YamlLimits.HARD_DEPTH)) }
        }

        test("counts the wrapper of a wrapped polymorphic value as a level") {
            val wrapped = yamlFormat {
                polymorphism = YamlPolymorphism.Wrapped
                yaml { limits { maximumDepth = 2 } }
            }

            shouldThrow<SerializationException> { wrapped.encodeToString(Labelled.serializer(), Labelled.Tagged("x")) }
            codes(wrapped.decodeOrFailure<Labelled>(yamlMapping { "tagged" to yamlMapping { "type" to "x" } })) shouldBe
                listOf(YamlDiagnosticCode.TooDeep)
        }
    }

    context("alias expansion") {
        test("decodes aliases that stay within the budget") {
            val text = "children: [&leaf {children: []}, *leaf, *leaf]"

            YamlFormat.Default.decodeFromString<Tree>(text) shouldBe Tree(List(COPIES) { Tree() })
        }

        test("refuses a node graph whose shared nodes expand beyond the budget") {
            val decoding = small.decodeOrFailure<Tree>(shared(LEVELS))

            codes(decoding) shouldContainExactly listOf(YamlDiagnosticCode.TooManyAliasExpansions)
            decoding.diagnostics.single().message shouldBe
                "The value expands to more than 100 nodes through aliases and shared nodes"
        }

        test("refuses a graph beyond a small budget early") {
            codes(small.decodeOrFailure<Tree>(shared(2))) shouldBe listOf(YamlDiagnosticCode.TooManyAliasExpansions)
            small.decodeOrFailure<Tree>(shared(1)).shouldBeInstanceOf<YamlDecoding.Decoded<Tree>>()
        }

        test("shares one budget between the documents of a stream") {
            val budget = WalkBudget(maximumDepth = DEPTH, allowance = 3)
            val first = DecodeContext(small.configuration, budget)
            val second = DecodeContext(small.configuration, budget)
            val node = yamlSequence(listOf(yamlScalar(1)))

            first
                .decodeRoot(ListSerializer(Int.serializer()), node, emptyList())
                .shouldBeInstanceOf<YamlDecoding.Decoded<List<Int>>>()
            codes(second.decodeRoot(ListSerializer(Int.serializer()), node, emptyList())) shouldBe
                listOf(YamlDiagnosticCode.TooManyAliasExpansions)
        }

        test("refuses to encode a value with more nodes than the budget") {
            val repeated = List(FEW) { "x" }

            shouldThrow<SerializationException> { small.encodeToString(repeated) }.message shouldBe
                "The value encodes to more than $FEW nodes"
        }
    }

    context("diagnostics") {
        test("collects at most the limit of problems") {
            val capped = yamlFormat { yaml { limits { maximumDiagnostics = DIAGNOSTICS } } }
            val keys = (1..UNKNOWN_KEYS).map { index ->
                YamlEntry(
                    key = yamlScalar("extra$index"),
                    value = yamlScalar(index),
                )
            }
            val node = yamlMapping(listOf(YamlEntry(key = yamlScalar("port"), value = yamlScalar(1))) + keys)

            codes(capped.decodeOrFailure<Listener>(node)) shouldBe
                List(DIAGNOSTICS) { YamlDiagnosticCode.UnknownProperty } + YamlDiagnosticCode.TooManyDiagnostics
        }
    }

    context("names") {
        test("refuses an alias that collides with the name of another property") {
            val failure = shouldThrow<IllegalStateException> { YamlFormat.Default.decodeFromString<Clash>("port: 1") }

            val name = Clash.serializer().descriptor.serialName

            failure.message shouldBe """The class "$name" uses the YAML name "port" for two properties"""
            shouldThrow<IllegalStateException> { YamlFormat.Default.encodeToString(Clash()) }
        }

        test("refuses two properties that share an alias") {
            shouldThrow<IllegalStateException> { YamlFormat.Default.decodeOrFailure<AliasClash>("{}") }
        }

        test("refuses two properties that a naming strategy maps to one name") {
            val snake = yamlFormat { namingStrategy = YamlNamingStrategy.SnakeCase }

            shouldThrow<IllegalStateException> { snake.encodeToString(SnakeClash()) }
            YamlFormat.Default.decodeFromString<SnakeClash>(
                "{httpPort: 1, http_port: 2}",
            ) shouldBe SnakeClash(httpPort = 1, other = 2)
        }

        test("accepts an alias that repeats the own name of its property") {
            YamlFormat.Default.decodeFromString<SelfAlias>("amount: $AMOUNT") shouldBe SelfAlias(AMOUNT)
        }

        test("refuses a property given under two of its names") {
            val decoding = YamlFormat.Default.decodeOrFailure<Renamed>("maxConnections: 1\ntimeout: 2\ntime-out: 3")

            codes(decoding) shouldBe listOf(YamlDiagnosticCode.DuplicateKey)
            decoding.diagnostics.single().message shouldBe """The property "time-out" is given more than once"""
        }

        test("refuses two keys that decode to the same map key") {
            val text = """{host: a, labels: {1: a, "1": b}}"""
            val decoding = YamlFormat.Default.decodeOrFailure<Server>(text)

            codes(decoding) shouldBe listOf(YamlDiagnosticCode.DuplicateKey)
            decoding.diagnostics.single().message shouldBe "The key ${SafeText.REDACTED} is given more than once"
            val quoted = yamlFormat { isValueQuoted = true }.decodeOrFailure<Server>(text)
            quoted.diagnostics.single().message shouldBe """The key "1" is given more than once"""
        }

        test("refuses a subtype with a property named like the class discriminator") {
            shouldThrow<IllegalStateException> {
                YamlFormat.Default.encodeToString(Labelled.serializer(), Labelled.Tagged("x"))
            }.message shouldBe """The class discriminator "type" collides with a property of the type "tagged""""
        }

        test("reports a missing property under its YAML name") {
            val snake = yamlFormat { namingStrategy = YamlNamingStrategy.SnakeCase }

            snake
                .decodeOrFailure<Renamed>("{}")
                .diagnostics
                .single()
                .message shouldBe "Missing max_connections"
        }
    }

    context("null nodes") {
        test("decodes a missing property without charging the budget") {
            val tight = yamlFormat {
                hasExplicitNulls = false
                yaml {
                    limits {
                        maximumNodes = 1
                        maximumAliasExpansion = 1
                    }
                }
            }

            tight.decodeFromNode<Nullable>(yamlMapping(emptyList())) shouldBe
                Nullable(null)
            val listener = yamlMapping {
                "port" to 1
                "protocol" to "tcp"
            }

            codes(tight.decodeOrFailure<Listener>(listener)) shouldBe listOf(YamlDiagnosticCode.TooManyAliasExpansions)
        }
    }
})
