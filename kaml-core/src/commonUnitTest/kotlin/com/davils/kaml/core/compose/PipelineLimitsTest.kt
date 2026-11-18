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

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.core.parser.SyntaxParser
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.source.YamlEncoding
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.source.encodedAs
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val INPUT_LIMIT: Int = 8
private const val NUMBER_LIMIT: Int = 5
private const val DIAGNOSTIC_LIMIT: Int = 3
private const val NODE_LIMIT: Long = 3L
private const val SCALAR_LIMIT: Int = 3
private const val DEPTH_LIMIT: Int = 3

class PipelineLimitsTest : FunSpec({
    context("the input length") {
        val tiny = yaml { limits { maximumInputLength = INPUT_LIMIT } }
        val atLimit = "a".repeat(INPUT_LIMIT)
        val pastLimit = "a".repeat(INPUT_LIMIT + 1)

        test("accepts a text at the limit") {
            tiny.root(atLimit).string() shouldBe atLimit
        }

        test("refuses a text past the limit") {
            tiny.problems(pastLimit) shouldContainExactly listOf(YamlDiagnosticCode.InputTooLong)
        }

        test("bounds the text that the syntax tree reads") {
            tiny.pipeline.readText(atLimit).isComplete shouldBe true
            val refused = tiny.pipeline.readText(pastLimit)

            refused.isComplete shouldBe false
            refused.diagnostics.map { it.code } shouldContainExactly listOf(YamlDiagnosticCode.InputTooLong)
        }

        test("bounds the tree that is composed") {
            val root = SyntaxParser.parse(pastLimit).root
            val result = tiny.pipeline.composeTree(pastLimit, root, emptyList())

            result.diagnostics.map { it.code } shouldContainExactly listOf(YamlDiagnosticCode.InputTooLong)
        }

        test("accepts bytes that decode to a text at the limit") {
            val bytes = atLimit.encodedAs(YamlEncoding.Utf32BigEndian)

            tiny.parse(YamlSource.of(bytes)).shouldBeInstanceOf<YamlParseResult.Parsed>()
        }

        test("refuses bytes past the limit before it decodes them") {
            val bytes = pastLimit.encodedAs(YamlEncoding.Utf32BigEndian)

            tiny.parse(YamlSource.of(bytes)).diagnostics.map { it.code } shouldContainExactly
                listOf(YamlDiagnosticCode.InputTooLong)
        }

        test("refuses bytes that decode to a text past the limit") {
            val bytes = pastLimit.encodeToByteArray()

            tiny.parse(YamlSource.of(bytes)).diagnostics.map { it.code } shouldContainExactly
                listOf(YamlDiagnosticCode.InputTooLong)
        }
    }

    context("the number length") {
        val short = yaml { limits { maximumNumberLength = NUMBER_LIMIT } }

        test("converts a number at the limit") {
            short.root("12345").string() shouldBe "12345"
            short.problems("1.234") shouldNotContain YamlDiagnosticCode.NumberTooLong
        }

        test("refuses a number past the limit") {
            short.problems("123456") shouldContain YamlDiagnosticCode.NumberTooLong
            short.problems("1.2345") shouldContain YamlDiagnosticCode.NumberTooLong
        }

        test("never converts a string") {
            short.problems("\"123456\"") shouldNotContain YamlDiagnosticCode.NumberTooLong
        }
    }

    context("the diagnostics") {
        val json = yaml {
            schema = YamlSchema.Json
            limits { maximumDiagnostics = DIAGNOSTIC_LIMIT }
        }
        val capped = yaml { limits { maximumDiagnostics = DIAGNOSTIC_LIMIT } }

        test("are all reported up to the limit") {
            json.problems("[a, b, c]") shouldContainExactly List(DIAGNOSTIC_LIMIT) { YamlDiagnosticCode.NotJson }
        }

        test("end with a count past the limit") {
            json.problems("[a, b, c, d]") shouldContainExactly
                List(DIAGNOSTIC_LIMIT) { YamlDiagnosticCode.NotJson } + YamlDiagnosticCode.TooManyDiagnostics
        }

        test("are capped while the source is parsed") {
            val source = List(DIAGNOSTIC_LIMIT + 2) { "\u0001" }.joinToString(" x ")

            capped.problems(source) shouldContainExactly
                List(DIAGNOSTIC_LIMIT) { YamlDiagnosticCode.InvalidCharacter } + YamlDiagnosticCode.TooManyDiagnostics
        }

        test("are capped while the events are read") {
            val source = List(
                DIAGNOSTIC_LIMIT + 2,
            ) { index -> "!h$index!x v" }.joinToString(separator = ", ", prefix = "[", postfix = "]")

            capped.problems(source) shouldContainExactly
                List(DIAGNOSTIC_LIMIT) { YamlDiagnosticCode.UnknownTagHandle } + YamlDiagnosticCode.TooManyDiagnostics
        }

        test("are capped while the graph is composed") {
            val tagged = yaml { limits { maximumDiagnostics = DIAGNOSTIC_LIMIT } }
            val source = List(
                DIAGNOSTIC_LIMIT + 2,
            ) { "!!int x" }.joinToString(separator = ", ", prefix = "[", postfix = "]")

            tagged.problems(source) shouldContainExactly
                List(DIAGNOSTIC_LIMIT) { YamlDiagnosticCode.InvalidTaggedValue } + YamlDiagnosticCode.TooManyDiagnostics
        }
    }

    context("the events") {
        test("hold up to the node limit") {
            val root = yaml { limits { maximumNodes = NODE_LIMIT } }.root("[a, b]")

            root[1].string() shouldBe "b"
        }

        test("stop past the node limit") {
            yaml { limits { maximumNodes = NODE_LIMIT } }.problems("[a, b, c]") shouldContain
                YamlDiagnosticCode.TooManyNodes
        }

        test("hold a scalar at the length limit") {
            val root = yaml { limits { maximumScalarLength = SCALAR_LIMIT } }.root("abc")

            root.string() shouldBe "abc"
        }

        test("hold up to the document limit") {
            val stream = text("a", "---", "b")

            yaml { limits { maximumDocuments = 2 } }.problems(stream) shouldNotContain
                YamlDiagnosticCode.TooManyDocuments
            yaml { limits { maximumDocuments = 1 } }.problems(stream) shouldContain YamlDiagnosticCode.TooManyDocuments
        }

        test("refuse a tree that nests past the depth limit") {
            val deep = "[[[a]]]"
            val shallow = yaml { limits { maximumDepth = DEPTH_LIMIT } }
            val root = SyntaxParser.parse(deep).root
            val result = shallow.pipeline.composeTree(deep, root, emptyList())

            result.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.TooDeep
            shallow.pipeline
                .composeTree("[[a]]", SyntaxParser.parse("[[a]]").root, emptyList())
                .shouldBeInstanceOf<YamlParseResult.Parsed>()
        }

        test("count a flow pair as a level") {
            val shallow = yaml { limits { maximumDepth = DEPTH_LIMIT } }

            shallow.problems("[a: b]") shouldNotContain YamlDiagnosticCode.TooDeep
            shallow.problems("[[a: b]]") shouldContain YamlDiagnosticCode.TooDeep
        }
    }
})
