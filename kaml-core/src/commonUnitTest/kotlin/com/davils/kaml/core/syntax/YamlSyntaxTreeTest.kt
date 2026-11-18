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

package com.davils.kaml.core.syntax

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.lines
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.core.syntax.fixture.deepMappings
import com.davils.kaml.core.syntax.fixture.deepSequences
import com.davils.kaml.core.syntax.fixture.hostileTexts
import com.davils.kaml.core.syntax.fixture.strayNodeTree
import com.davils.kaml.core.syntax.fixture.streamPieces
import com.davils.kaml.core.syntax.fixture.streamTexts
import com.davils.kaml.core.yaml
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private const val CASES: Int = 100
private const val NESTED_PORT: String = "8080"
private const val MAPPING_ENTRIES: Int = 5
private const val SMALL_INPUT: Int = 16
private const val SHORT_SCALAR: Int = 3
private const val TOKENS_PER_LEVEL: Int = 4
private const val NODES_PER_LEVEL: Int = 5
private const val ANCESTORS_PER_LEVEL: Int = 3
private const val DEEPER: Int = 10
private const val EDIT_CASES: Int = 5_000
private const val MOST_OFFSET: Int = 400
private const val WIDEST_EDIT: Int = 6
private const val THREE_DOCUMENTS: Int = 3
private const val NINE_NODES: Long = 9L
private const val B_ENTRY: Int = 4

private val replacements: Arb<String> = Arb.element(streamPieces + "")

private fun scalarsOf(value: YamlValueSyntax): List<String> = when (value) {
    is YamlValueSyntax.Scalar -> listOf(value.value)
    is YamlValueSyntax.Empty -> listOf("")
    is YamlValueSyntax.Alias -> emptyList()
    is YamlValueSyntax.Sequence -> value.items.flatMap(::scalarsOf)
    is YamlValueSyntax.Mapping -> value.entries.flatMap(::entryScalars)
}

private fun entryScalars(entry: YamlEntrySyntax): List<String> {
    val values = entry.value?.let(::scalarsOf) ?: listOf("")
    return scalarsOf(entry.key) + values
}

private fun documentScalars(tree: YamlSyntaxTree): List<String> =
    tree.documents.flatMap { document -> scalarsOf(document.root) }

private const val EDITED_B: String = "b: 20"

class YamlSyntaxTreeTest : FunSpec({
    context("incremental reparsing") {
        test("reuses the documents it does not touch") {
            val text = lines("a: 1", "---", "b: 2", "---", "c: 3")
            val tree = YamlSyntaxTree.parse(text)
            val at = text.indexOf('2')
            val edited = tree.edit(listOf(TextEdit(TextRange(start = at, end = at + 1), "20")))

            edited.text shouldContain EDITED_B
            edited.documents[0].node shouldBe tree.documents[0].node
            edited.documents[2].node shouldNotBe tree.documents[2].node
        }

        test("parses everything again without edits") {
            val tree = YamlSyntaxTree.parse("a: 1")

            tree.edit(emptyList()).text shouldBe "a: 1"
        }

        test("parses everything again when an edit turns a document into a marker") {
            val text = lines("a: 1", "...", ".b", "")
            val at = text.indexOf("b")
            val edited = YamlSyntaxTree.parse(text).edit(listOf(TextEdit(TextRange(start = at, end = at + 1), "..")))

            edited.text shouldBe lines("a: 1", "...", "...", "")
            edited.documents.size shouldBe 1
        }

        test("applies several edits at once") {
            val text = lines("a: 1", "b: 2", "---", "c: 3")
            val first = text.indexOf('1')
            val second = text.indexOf('2')
            val edits = listOf(
                TextEdit(TextRange(start = first, end = first + 1), "10"),
                TextEdit(TextRange(start = second, end = second + 1), "20"),
            )
            val edited = YamlSyntaxTree.parse(text).edit(edits)

            edited.text shouldBe lines("a: 10", EDITED_B, "---", "c: 3")
            edited.diagnostics shouldBe emptyList()
        }

        test("applies edits in any order") {
            val text = lines("a: 1", "b: 2")
            val first = text.indexOf('1')
            val second = text.indexOf('2')
            val edits = listOf(
                TextEdit(TextRange(start = second, end = second + 1), "20"),
                TextEdit(TextRange(start = first, end = first + 1), "10"),
            )

            YamlSyntaxTree.parse(text).edit(edits).text shouldBe lines("a: 10", EDITED_B)
        }

        test("reparses documents after carriage returns") {
            val text = "a: 1\r---\rb: 2\r"
            val at = text.indexOf('2')
            val edited = YamlSyntaxTree.parse(text).edit(listOf(TextEdit(TextRange(start = at, end = at + 1), "3")))

            edited.text shouldBe "a: 1\r---\rb: 3\r"
            edited.documents.size shouldBe 2
        }

        test("parses the next document again when an edit joins its marker to the line before it") {
            val joined = YamlSyntaxTree.parse("|\n--- b\n").edit(listOf(TextEdit(TextRange(start = 1, end = 2), " # ")))
            val refused = YamlSyntaxTree.parse("|\n--- |\n").edit(listOf(TextEdit(TextRange(start = 1, end = 2), "  ")))

            joined.text shouldBe "| # --- b\n"
            joined.green shouldBe YamlSyntaxTree.parse(joined.text).green
            joined.documents.size shouldBe 1
            refused.compose().shouldBeInstanceOf<YamlParseResult.Failed>()
            refused.diagnostics shouldBe YamlSyntaxTree.parse(refused.text).diagnostics
        }

        test("gives the tree and the diagnostics of a full parse for any edit") {
            checkAll(
                iterations = EDIT_CASES,
                genA = streamTexts,
                genB = Arb.int(0..MOST_OFFSET),
                genC = Arb.int(0..WIDEST_EDIT),
                genD = replacements,
            ) { text, position, width, replacement ->
                val start = position % (text.length + 1)
                val end = minOf(a = text.length, b = start + width)
                val edited = YamlSyntaxTree.parse(text).edit(listOf(TextEdit(TextRange(start, end), replacement)))
                val full = YamlSyntaxTree.parse(edited.text)

                edited.green shouldBe full.green
                edited.diagnostics.map { it.code to it.range } shouldBe full.diagnostics.map { it.code to it.range }
                edited.compose() shouldBe full.compose()
            }
        }

        test("keeps tokens between documents") {
            val text = lines("", "a: 1", "...", "...", "---", "b: 2")
            val at = text.indexOf('2')
            val edited = YamlSyntaxTree.parse(text).edit(listOf(TextEdit(TextRange(start = at, end = at + 1), "3")))

            edited.documents.size shouldBe 2
            edited.text shouldContain "b: 3"
        }
    }

    context("the typed view") {
        val tree = YamlSyntaxTree.parse(
            lines(
                "# The server",
                "server: &s !srv",
                "  # The port",
                "  port: 8080 # default",
                "  hosts: [a, {b: c}]",
                "  ? x",
                "copy: *s",
            ),
        )
        val document = tree.documents.single()
        val root = document.root.shouldBeInstanceOf<YamlValueSyntax.Mapping>()
        val serverEntry = root.entry("server").shouldNotBeNull()
        val server = serverEntry.value.shouldBeInstanceOf<YamlValueSyntax.Mapping>()
        val port = server.entry("port").shouldNotBeNull()

        test("reads entries, properties and comments") {
            server.anchor shouldBe "s"
            server.tag shouldBe "!srv"
            server.isFlow shouldBe false
            port.value.shouldBeInstanceOf<YamlValueSyntax.Scalar>().value shouldBe NESTED_PORT
            port.leadingComments shouldBe listOf("The port")
            port.trailingComment shouldBe "default"
            serverEntry.leadingComments shouldBe listOf("The server")
            serverEntry.trailingComment shouldBe null
            server.entry("x")?.value shouldBe null
            server.entry("missing") shouldBe null
            port.value?.anchor shouldBe null
            port.value?.tag shouldBe null
        }

        test("reads keys that are no scalars") {
            val complex = YamlSyntaxTree.parse(lines("? [a]", ": 1", ": 2", "!t x: &k y"))
            val mapping = complex.documents
                .single()
                .root
                .shouldBeInstanceOf<YamlValueSyntax.Mapping>()
            val tagged = mapping.entries.last()

            mapping.entries.map { it.keyText } shouldBe listOf(null, null, "x")
            tagged.key.tag shouldBe "!t"
            tagged.key.anchor shouldBe null
            tagged.value?.anchor shouldBe "k"
        }

        test("reads the scalars of every document in order") {
            val stream = YamlSyntaxTree.parse(lines("a: [x, {y: z}]", "---", "- w", "- *r", "-", "---", "? k", ""))

            documentScalars(stream) shouldBe listOf("a", "x", "y", "z", "w", "", "k", "")
        }

        test("reads the name of an alias that has properties by mistake") {
            val broken = YamlSyntaxTree.parse("x: &a *b")
            val mapping = broken.documents
                .single()
                .root
                .shouldBeInstanceOf<YamlValueSyntax.Mapping>()

            mapping
                .entry("x")
                ?.value
                .shouldBeInstanceOf<YamlValueSyntax.Alias>()
                .name shouldBe "b"
        }

        test("reads only the documents of a stream") {
            strayNodeTree().documents.size shouldBe 1
        }

        test("reads aliases") {
            val copy = root.entry("copy").shouldNotBeNull()

            copy.value.shouldBeInstanceOf<YamlValueSyntax.Alias>().name shouldBe "s"
            copy.keyText shouldBe "copy"
        }

        test("reads flow collections") {
            val hosts = server.entry("hosts")?.value.shouldBeInstanceOf<YamlValueSyntax.Sequence>()

            hosts.isFlow shouldBe true
            hosts.items[1].shouldBeInstanceOf<YamlValueSyntax.Mapping>().isFlow shouldBe true
        }

        test("reads scalar styles") {
            val styled = YamlSyntaxTree.parse(lines("a: 'x'", "b: \"y\"", "c: |", "  z", "d: >", "  w"))
            val mapping = styled.documents
                .single()
                .root
                .shouldBeInstanceOf<YamlValueSyntax.Mapping>()
            val styles = mapping.entries.map { it.value.shouldBeInstanceOf<YamlValueSyntax.Scalar>().style }

            styles shouldBe listOf(
                YamlScalarStyle.SingleQuoted,
                YamlScalarStyle.DoubleQuoted,
                YamlScalarStyle.Literal,
                YamlScalarStyle.Folded,
            )
            mapping
                .entry("c")
                ?.value
                .shouldBeInstanceOf<YamlValueSyntax.Scalar>()
                .value shouldBe "z\n"
        }

        test("reads documents with markers and directives") {
            val marked = YamlSyntaxTree.parse(lines("%YAML 1.2", "---", "a", "...", "---", ""))
            val first = marked.documents.first()

            first.hasExplicitStart shouldBe true
            first.hasExplicitEnd shouldBe true
            first.directives.size shouldBe 1
            marked.documents
                .last()
                .root
                .shouldBeInstanceOf<YamlValueSyntax.Empty>()
            document.hasExplicitStart shouldBe false
            document.hasExplicitEnd shouldBe false
            document.directives shouldBe emptyList()
        }
    }

    context("the tree") {
        val tree = YamlSyntaxTree.parse(lines("server:", "  port: 8080", "  host: a", "  b: c", "  d: e", "  f: g"))

        test("navigates tokens and nodes") {
            val token = tree.root.tokenAt(tree.text.indexOf(NESTED_PORT)).shouldNotBeNull()

            token.kind shouldBe YamlSyntaxKind.PLAIN_SCALAR
            token.text shouldBe NESTED_PORT
            token.ancestors.first().kind shouldBe YamlSyntaxKind.SCALAR
            tree.root.coveringNode(token.range).kind shouldBe YamlSyntaxKind.SCALAR
            tree.root.descendants.count { it.kind == YamlSyntaxKind.BLOCK_MAPPING_ENTRY } shouldBe
                MAPPING_ENTRIES + 1
            tree.root.tokenAt(tree.text.length) shouldBe null
            tree.root.tokenAt(-1) shouldBe null
            token.ancestors.last().kind shouldBe YamlSyntaxKind.STREAM
            tree.root.coveringNode(TextRange(start = 0, end = tree.text.length + 1)).kind shouldBe YamlSyntaxKind.STREAM
        }

        test("compares elements by position") {
            val first = YamlSyntaxTree.parse(tree.text)
            val token = tree.root.tokenAt(0).shouldNotBeNull()

            tree.root shouldBe tree.root
            tree.root.hashCode() shouldBe tree.root.hashCode()
            tree.root shouldNotBe first.root
            token shouldBe tree.root.tokenAt(0)
            token.hashCode() shouldBe tree.root.tokenAt(0).hashCode()
            token shouldNotBe first.root.tokenAt(0)
            token shouldNotBe tree.root
            tree.root shouldNotBe token
            val twinTree = YamlSyntaxTree.parse("a: a")
            val twins = twinTree.root.tokens.filter { it.kind == YamlSyntaxKind.PLAIN_SCALAR }
            twins.first() shouldNotBe twins.last()
            val entries = TextRange(start = tree.text.indexOf("port"), end = tree.text.indexOf("host"))
            tree.root.coveringNode(entries).kind shouldBe YamlSyntaxKind.BLOCK_MAPPING
            token.toString() shouldContain "PLAIN_SCALAR"
            tree.root.toString() shouldContain "STREAM"
            tree.toString() shouldContain "characters"
        }

        test("finds children by kind") {
            val document = tree.root.child(YamlSyntaxKind.DOCUMENT).shouldNotBeNull()

            document.child(YamlSyntaxKind.ERROR) shouldBe null
            document.token(YamlSyntaxKind.DOCUMENT_START) shouldBe null
        }

        test("composes the stream it holds") {
            tree.compose().shouldBeInstanceOf<YamlParseResult.Parsed>()
        }

        test("keeps a copy of its diagnostics") {
            val broken = YamlSyntaxTree.parse("a: [1")

            broken.diagnostics.map { it.code } shouldContainCode YamlDiagnosticCode.UnterminatedFlowCollection
        }
    }

    context("any text") {
        test("is parsed without loss and without an exception") {
            checkAll(CASES, hostileTexts) { text ->
                val parsed = YamlSyntaxTree.parse(text)

                parsed.root.text shouldBe text
                parsed.root.tokens.joinToString("") { it.text } shouldBe text
            }
        }
    }

    context("the input limit") {
        val small = yaml { limits { maximumInputLength = SMALL_INPUT } }

        test("refuses a text longer than the limit before reading it") {
            val text = "a: 1\n".repeat(SMALL_INPUT)
            val tree = YamlSyntaxTree.parse(text, small)

            tree.diagnostics.map { it.code } shouldBe listOf(YamlDiagnosticCode.InputTooLong)
            tree.root.children.shouldBeEmpty()
            tree.documents.shouldBeEmpty()
            tree.text shouldBe text
        }

        test("reads a text as long as the limit") {
            val text = lines("a: 1", "b: 2", "c: 33", "")
            val tree = YamlSyntaxTree.parse(text, small)

            text.length shouldBe SMALL_INPUT
            tree.diagnostics.shouldBeEmpty()
            tree.documents.size shouldBe 1
        }

        test("refuses an incremental edit whose result is longer than the limit") {
            val text = lines("a: 1", "---", "b: 2", "")
            val tree = YamlSyntaxTree.parse(text, small)
            val at = text.indexOf('2')
            val edited = tree.edit(listOf(TextEdit(TextRange(start = at, end = at + 1), "2".repeat(SMALL_INPUT))))

            edited.diagnostics.map { it.code } shouldBe listOf(YamlDiagnosticCode.InputTooLong)
            edited.root.children.shouldBeEmpty()
            edited.text.length shouldBe text.length + SMALL_INPUT - 1
        }

        test("reads an incremental edit whose result is as long as the limit") {
            val text = lines("a: 1", "---", "b: 2", "")
            val at = text.indexOf('2')
            val tree = YamlSyntaxTree.parse(text, small)
            val edited = tree.edit(listOf(TextEdit(TextRange(start = at, end = at + 1), "222")))

            edited.text.length shouldBe SMALL_INPUT
            edited.diagnostics.shouldBeEmpty()
        }
    }

    context("other limits during an incremental edit") {
        test("parse everything again after a document reached a limit") {
            val limited = yaml { limits { maximumScalarLength = SHORT_SCALAR } }
            val text = lines("a: long", "---", "b: 1", "")
            val tree = YamlSyntaxTree.parse(text, limited)
            val at = text.indexOf('1')
            val edited = tree.edit(listOf(TextEdit(TextRange(start = at, end = at + 1), "2")))
            val full = YamlSyntaxTree.parse(edited.text, limited)

            tree.diagnostics.map { it.code } shouldContainCode YamlDiagnosticCode.ScalarTooLong
            edited.diagnostics.map { it.code to it.range } shouldBe full.diagnostics.map { it.code to it.range }
            edited.documents[0].node shouldNotBe tree.documents[0].node
        }

        test("parse everything again when the diagnostics would exceed their limit") {
            val few = yaml { limits { maximumDiagnostics = 1 } }
            val text = lines("a: 'x' y", "---", "b: 1", "")
            val tree = YamlSyntaxTree.parse(text, few)
            val at = text.indexOf('1')
            val edited = tree.edit(listOf(TextEdit(TextRange(start = at, end = at + 1), "'z' w")))
            val full = YamlSyntaxTree.parse(edited.text, few)

            edited.diagnostics.map { it.code to it.range } shouldBe full.diagnostics.map { it.code to it.range }
            edited.documents[0].node shouldNotBe tree.documents[0].node
        }

        test("charge the document limit over the whole stream") {
            val limited = yaml { limits { maximumDocuments = THREE_DOCUMENTS } }
            val text = lines("a: 1", "---", "b: 2", "---", "c: 3", "")
            val at = text.indexOf("b: 2")
            val tree = YamlSyntaxTree.parse(text, limited)
            val edited = tree.edit(listOf(TextEdit(TextRange(start = at, end = at + B_ENTRY), "b: 2\n---\nd: 4")))
            val full = YamlSyntaxTree.parse(edited.text, limited)

            tree.diagnostics.shouldBeEmpty()
            edited.diagnostics.map { it.code to it.range } shouldBe full.diagnostics.map { it.code to it.range }
            edited.diagnostics.map { it.code } shouldContainCode YamlDiagnosticCode.TooManyDocuments
        }

        test("charge the node limit over the whole stream") {
            val limited = yaml { limits { maximumNodes = NINE_NODES } }
            val text = lines("a: 1", "---", "b: 2", "---", "c: 3", "")
            val at = text.indexOf("b: 2")
            val tree = YamlSyntaxTree.parse(text, limited)
            val edited = tree.edit(listOf(TextEdit(TextRange(start = at, end = at + B_ENTRY), "b: [1, 2, 3]")))
            val full = YamlSyntaxTree.parse(edited.text, limited)

            tree.diagnostics.shouldBeEmpty()
            edited.diagnostics.map { it.code to it.range } shouldBe full.diagnostics.map { it.code to it.range }
            edited.diagnostics.map { it.code } shouldContainCode YamlDiagnosticCode.TooManyNodes
        }

        test("refuses edits that overlap or reach beyond the text") {
            val tree = YamlSyntaxTree.parse("a: 1")

            val beyond = TextRange(start = 0, end = tree.text.length + 1)
            val first = TextRange(start = 0, end = 2)
            val overlapping = TextRange(start = 1, end = tree.text.length)

            shouldThrow<IllegalArgumentException> { tree.edit(listOf(TextEdit(beyond, ""))) }
            shouldThrow<IllegalArgumentException> { tree.edit(listOf(TextEdit(first, ""), TextEdit(overlapping, ""))) }
        }
    }

    context("trees nested to the depth limit") {
        val levels = YamlLimits.HARD_DEPTH
        val tree = deepMappings(levels)
        val innermost = tree.text.length - 1

        test("walk their tokens and nodes without exhausting the stack") {
            tree.root.text shouldBe tree.text
            tree.root.tokens.count() shouldBe TOKENS_PER_LEVEL * levels
            tree.root.descendants.count() shouldBe NODES_PER_LEVEL * levels + 2
            tree.root.descendants
                .last()
                .kind shouldBe YamlSyntaxKind.SCALAR
        }

        test("find the innermost token and node without exhausting the stack") {
            val token = tree.root.tokenAt(innermost).shouldNotBeNull()

            token.text shouldBe "b"
            tree.root.coveringNode(token.range).kind shouldBe YamlSyntaxKind.SCALAR
            token.ancestors.count() shouldBe ANCESTORS_PER_LEVEL * levels + ANCESTORS_PER_LEVEL
        }

        test("read the typed view down to the innermost value") {
            var value = tree.documents.single().root
            repeat(levels) {
                val mapping = value.shouldBeInstanceOf<YamlValueSyntax.Mapping>()
                value = mapping.entry("a")?.value.shouldNotBeNull()
            }

            value.shouldBeInstanceOf<YamlValueSyntax.Scalar>().value shouldBe "b"
        }

        test("walk flow collections nested far deeper than the limit") {
            val deep = deepSequences(levels * DEEPER)

            deep.root.text shouldBe deep.text
            deep.root.descendants.count() shouldBe levels * DEEPER + 1
            deep.root.tokenAt(deep.text.length / 2)?.kind shouldBe YamlSyntaxKind.FLOW_SEQUENCE_END
        }
    }
})

private infix fun List<YamlDiagnosticCode>.shouldContainCode(code: YamlDiagnosticCode) {
    (code in this) shouldBe true
}
