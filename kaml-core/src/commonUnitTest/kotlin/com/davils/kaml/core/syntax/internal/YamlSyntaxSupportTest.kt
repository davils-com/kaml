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

package com.davils.kaml.core.syntax.internal

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.fixture.deepMappings
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val UNCLOSED: String = "a: [1"
private const val WIDE: Int = 2

@OptIn(DavilsInternal::class)
class YamlSyntaxSupportTest : FunSpec({
    val yaml = Yaml.Default

    context("parse") {
        test("returns the lossless tree of a text") {
            val parsed = YamlSyntaxSupport.parse("a: 1 # note", yaml)

            parsed.root.text shouldBe "a: 1 # note"
            parsed.diagnostics shouldBe emptyList()
        }

        test("reports the problems of a text") {
            val parsed = YamlSyntaxSupport.parse(UNCLOSED, yaml)

            parsed.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.UnterminatedFlowCollection
        }
    }

    context("compose") {
        test("composes a tree into a stream") {
            val parsed = YamlSyntaxSupport.parse("a: 1", yaml)
            val result = YamlSyntaxSupport.compose(
                yaml = yaml,
                text = "a: 1",
                root = parsed.root,
                diagnostics = parsed.diagnostics,
            )

            val parsedResult = result.shouldBeInstanceOf<YamlParseResult.Parsed>()

            parsedResult.stream.documents.size shouldBe 1
        }

        test("keeps the problems of the tree") {
            val parsed = YamlSyntaxSupport.parse(UNCLOSED, yaml)
            val result = YamlSyntaxSupport.compose(
                yaml = yaml,
                text = UNCLOSED,
                root = parsed.root,
                diagnostics = parsed.diagnostics,
            )

            val failed = result.shouldBeInstanceOf<YamlParseResult.Failed>()

            failed.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.UnterminatedFlowCollection
        }
    }

    context("scalar values") {
        test("reads flow scalars of every style") {
            YamlSyntaxSupport.flowScalarValue(YamlSyntaxKind.PLAIN_SCALAR, "a\n  b") shouldBe "a b"
            YamlSyntaxSupport.flowScalarValue(YamlSyntaxKind.SINGLE_QUOTED_SCALAR, "'it''s'") shouldBe "it's"
            YamlSyntaxSupport.flowScalarValue(YamlSyntaxKind.DOUBLE_QUOTED_SCALAR, """"a\tb"""") shouldBe "a\tb"
        }

        test("returns the text of a token that is no scalar") {
            YamlSyntaxSupport.flowScalarValue(YamlSyntaxKind.COMMENT, "# c") shouldBe "# c"
        }

        test("reads block scalars") {
            YamlSyntaxSupport.blockScalarValue(header = "|", content = "  a\n  b\n", parentIndent = 0) shouldBe "a\nb\n"
            YamlSyntaxSupport.blockScalarValue(header = ">-", content = "  a\n  b\n", parentIndent = 0) shouldBe "a b"
        }
    }

    context("green elements") {
        test("expose their kind, length and text") {
            val parsed = YamlSyntaxSupport.parse("a: 1", yaml)
            val document = parsed.root.children.single()

            document.kind shouldBe YamlSyntaxKind.DOCUMENT
            document.length shouldBe "a: 1".length
            YamlSyntaxKind.COMMENT.isTrivia shouldBe true
            YamlSyntaxKind.DOCUMENT.isValueNode shouldBe false
        }

        test("compare and hash trees nested to the depth limit without exhausting the stack") {
            val levels = YamlLimits.HARD_DEPTH
            val first = deepMappings(levels).green
            val second = deepMappings(levels).green

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe deepMappings(levels, indent = WIDE).green
            first.text shouldBe deepMappings(levels).text
        }

        test("compare their structure and the text of their tokens") {
            val token = GreenToken(YamlSyntaxKind.PLAIN_SCALAR, "a")
            val scalar = GreenNode(YamlSyntaxKind.SCALAR, listOf(token))
            val other = GreenNode(YamlSyntaxKind.SCALAR, listOf(GreenToken(YamlSyntaxKind.PLAIN_SCALAR, "b")))
            val longer = GreenNode(YamlSyntaxKind.SCALAR, listOf(GreenToken(YamlSyntaxKind.PLAIN_SCALAR, "ab")))
            val split = GreenNode(YamlSyntaxKind.SCALAR, listOf(token, GreenToken(YamlSyntaxKind.PLAIN_SCALAR, "b")))

            GreenNode(YamlSyntaxKind.VALUE, listOf(scalar)) shouldBe GreenNode(YamlSyntaxKind.VALUE, listOf(scalar))
            scalar shouldNotBe other
            scalar shouldNotBe longer
            longer shouldNotBe split
            scalar shouldNotBe GreenNode(YamlSyntaxKind.ALIAS_NODE, listOf(token))
            scalar.equals(token) shouldBe false
            scalar.hashCode() shouldNotBe other.hashCode()
            scalar.toString() shouldBe "SCALAR(1 children, 1)"
        }
    }
})
