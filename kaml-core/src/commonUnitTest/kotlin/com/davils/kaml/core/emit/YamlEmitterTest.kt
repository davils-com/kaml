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

package com.davils.kaml.core.emit

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.stream
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.YamlStream
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.node.yamlTagged
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.yaml
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val WIDE_INDENT: Int = 4
private const val TOO_WIDE: Int = 10
private const val PORT: Long = 8080L
private const val YAML_DIRECTIVE: String = "%YAML 1.2"
private const val LEGACY_DIRECTIVE: String = "%YAML 1.1"
private const val SECOND_DOCUMENT: String = "--- b"
private const val INDENTED_ONE: String = "  one"
private const val INDENTED_TWO: String = "  two"
private const val LONG_KEY: Int = 2000
private const val LINE_PARTS: Int = 16
private const val LARGE: Double = 1e30
private const val SMALL: Double = 1e-30
private const val HALF: Double = 0.5
private const val OCTAL_VALUE: Long = 493L

private val SPECIAL: List<String> = listOf(
    "bell",
    "\u0007",
    " nul",
    "\u0000",
    " bom",
    "\uFEFF",
    " del",
    "\u007F",
    " line",
    "\u2028",
    " \"q\" ",
    "\\",
)

class YamlEmitterTest : FunSpec({
    val yaml = Yaml.Default

    context("block collections") {
        test("writes a mapping with an indented sequence") {
            val node = yamlMapping {
                "name" to "web"
                "ports" to yamlSequence {
                    add(PORT)
                    add(yamlMapping { "a" to 1 })
                }
            }

            yaml.emit(node) shouldBe text("name: web", "ports:", "  - 8080", "  - a: 1", "")
        }

        test("writes a sequence at the indentation of its key when asked to") {
            val flat = yaml { emitter { isSequenceIndented = false } }

            flat.emit(yamlMapping { "list" to yamlSequence { add("a") } }) shouldBe text("list:", "- a", "")
        }

        test("writes nested sequences compactly") {
            yaml.emit(yamlSequence { add(yamlSequence { add("a") }) }) shouldBe text("- - a", "")
        }

        test("writes empty collections in flow style") {
            yaml.emit(yamlMapping { "list" to yamlSequence {} }) shouldBe text("list: []", "")
            yaml.emit(yamlMapping { "map" to yamlMapping {} }) shouldBe text("map: {}", "")
        }

        test("keeps the flow style of a parsed collection") {
            yaml.emit(yaml.root("a: [1, {b: c}]")) shouldBe text("a: [1, {b: c}]", "")
        }

        test("drops the flow style when styles are not preserved") {
            val plain = yaml { emitter { isStylePreserved = false } }

            plain.emit(plain.root("a: [1]")) shouldBe text("a:", "  - 1", "")
        }

        test("writes a complex key") {
            val text = yaml.emit(yaml.root(text("? [a, b]", ": c", "? {x: y}", ": z")))

            yaml.root(text) shouldBe yaml.root(text("? [a, b]", ": c", "? {x: y}", ": z"))
        }

        test("writes a block collection as a key") {
            val node = yamlMapping { yamlMapping { "a" to yamlSequence { add("b") } } to yamlScalar("c") }

            yaml.root(yaml.emit(node)) shouldBe node
        }

        test("sorts keys when asked to") {
            val sorted = yaml { emitter { hasSortedKeys = true } }

            sorted.emit(
                yamlMapping {
                    "b" to 1
                    "a" to 2
                },
            ) shouldBe text("a: 2", "b: 1", "")
        }

        test("indents by the configured width") {
            val wide = yaml { emitter { indent = WIDE_INDENT } }

            wide.emit(yamlMapping { "a" to yamlMapping { "b" to 1 } }) shouldBe text("a:", "    b: 1", "")
        }

        test("refuses an indentation it cannot write") {
            shouldThrow<ValidationException> { yaml { emitter { indent = TOO_WIDE } } }
        }

        test("ends lines with a carriage return when asked to") {
            val windows = yaml { emitter { lineBreak = YamlLineBreak.CrLf } }

            windows.emit(yamlMapping { "a" to 1 }) shouldBe "a: 1\r\n"
        }
    }

    context("scalars") {
        test("quotes strings that would read as another type") {
            yaml.emit(
                yamlSequence {
                    add("true")
                    add("12")
                    add("null")
                    add("")
                },
            ) shouldBe
                text("- \"true\"", "- \"12\"", "- \"null\"", "- \"\"", "")
        }

        test("quotes strings that would break the structure") {
            val values = listOf("- a", "a: b", "a #b", "[a]", "a:", " a", "a ", "---", "*a", "?", "#", "@a", "%a")
            val node = yamlSequence(values.map { yamlScalar(it) })

            yaml.root(yaml.emit(node)) shouldBe node
        }

        test("prefers single quotes when asked to") {
            val single = yaml { emitter { quoteStyle = YamlQuoteStyle.Single } }

            single.emit(yamlScalar("true")) shouldBe text("'true'", "")
            single.emit(yamlScalar("it's")) shouldBe text("it's", "")
            single.emit(yamlScalar("12\t")) shouldBe text("'12\t'", "")
        }

        test("escapes characters that cannot be written as they are") {
            val node = yamlScalar(SPECIAL.joinToString(""))
            val written = yaml.emit(node)

            written shouldContain "\\a"
            written shouldContain "\\0"
            written shouldContain "\\uFEFF"
            written shouldContain "\\x7F"
            yaml.root(written) shouldBe node
        }

        test("writes a multi-line string as a literal block") {
            yaml.emit(yamlMapping { "text" to "one\ntwo\n" }) shouldBe text("text: |", INDENTED_ONE, INDENTED_TWO, "")
        }

        test("chooses the chomping of a literal block") {
            yaml.emit(yamlScalar("one\ntwo")) shouldBe text("|-", INDENTED_ONE, INDENTED_TWO, "")
            yaml.emit(yamlScalar("one\n\n")) shouldBe text("|+", INDENTED_ONE, "", "")
        }

        test("adds an indentation indicator for leading spaces") {
            val node = yamlMapping { "text" to "  indented\nline\n" }

            yaml.emit(node) shouldContain "|2"
            yaml.root(yaml.emit(node)) shouldBe node
        }

        test("keeps a requested quote style") {
            yaml.emit(yamlScalar("plain", YamlScalarStyle.SingleQuoted)) shouldBe text("'plain'", "")
            yaml.emit(yamlScalar("plain", YamlScalarStyle.DoubleQuoted)) shouldBe text("\"plain\"", "")
        }

        test("writes a folded scalar as a literal block") {
            yaml.emit(yamlScalar("a b\n", YamlScalarStyle.Folded)) shouldBe text("|", "  a b", "")
        }

        test("writes a null as an empty value or as null") {
            yaml.emit(yamlMapping { "a" to yamlNull() }) shouldBe text("a: null", "")
            yaml.emit(yaml.root("a:")) shouldBe text("a:", "")
            yaml.emit(yaml.root("[a, ~]")) shouldBe text("[a, ~]", "")
            yaml.emit(yaml.root("{a: }")) shouldBe text("{a: null}", "")
        }

        test("quotes YAML 1.1 words for older readers") {
            val compatible = yaml { emitter { compatibility = YamlVersion.V1_1 } }

            compatible.emit(
                yamlSequence {
                    add("yes")
                    add("0755")
                    add("on")
                },
            ) shouldBe
                text("- \"yes\"", "- \"0755\"", "- \"on\"", "")
        }

        test("writes typed values so that every reader of the version reads the same value") {
            val legacyReader = yaml { schema = YamlSchema.Yaml11 }
            val compatible = yaml { emitter { compatibility = YamlVersion.V1_1 } }
            val parsed = yaml.root("[0755, 010, 1e3, .5, True, ~, -0.0, 0o17, 0x1F]")
            val numbers = yamlSequence(
                listOf(LARGE, SMALL, Double.NaN, Double.NEGATIVE_INFINITY, HALF).map { number -> yamlScalar(number) },
            )

            listOf(parsed, numbers).forEach { node ->
                yaml.root(yaml.emit(node)) shouldBe node
                yaml.root(compatible.emit(node)) shouldBe node
                legacyReader.root(compatible.emit(node)) shouldBe node
            }
        }

        test("writes a value read as YAML 1.1 in a form that YAML 1.2 reads the same") {
            val stream = yaml().stream(text(LEGACY_DIRECTIVE, "---", "[0755, 1e3, 1.0e+3]"))

            yaml.emit(stream.documents.single().root) shouldBe text("[493, \"1e3\", 1.0e+3]", "")
        }

        test("writes a number too long to be read as one with its tag") {
            val digits = "1".repeat(YamlLimits.HARD_NUMBER_LENGTH + 1)

            yaml.emit(yamlTagged(digits, YamlTag.INTEGER)) shouldBe text("!!int $digits", "")
        }

        test("writes a document of YAML 1.1 for YAML 1.1 readers") {
            val root = yamlSequence(listOf(yamlScalar("yes"), yamlScalar("0755"), yamlScalar(OCTAL_VALUE)))
            val written = yaml.emit(YamlDocument(root, version = YamlVersion.V1_1))

            written shouldBe text(LEGACY_DIRECTIVE, "---", "- \"yes\"", "- \"0755\"", "- 493", "")
            yaml
                .stream(written)
                .documents
                .single()
                .root shouldBe root
        }

        test("writes a value of a YAML 1.1 type in its canonical form") {
            val stream = yaml().stream(text(LEGACY_DIRECTIVE, "---", "[yes, 0x_1F, 0b11, 1:00]"))
            val legacy = stream.documents.single().root

            yaml.emit(legacy) shouldBe text("[true, 31, 3, 60]", "")
        }
    }

    context("tags") {
        test("writes core, local and verbatim tags") {
            val node = yamlSequence {
                add(yamlTagged("10.0.0.1", YamlTag("!ipv4")))
                add(yamlTagged("x", YamlTag("tag:example.com,2000:app")))
                add(yamlTagged("x", YamlTag("!a b")))
            }
            val written = yaml.emit(node)

            written shouldBe text("- !ipv4 10.0.0.1", "- !<tag:example.com,2000:app> x", "- !a%20b x", "")
            yaml.root(written) shouldBe node
        }

        test("writes the tag of a tagged collection") {
            val written = yaml.emit(yaml.root(text("set: !!set {a, b}", "omap: !custom", "  - a")))

            written shouldContain "!!set"
            written shouldContain "!custom"
        }

        test("writes a tag for an integer that does not read as one") {
            yaml.emit(yaml.root("!!int '12'")) shouldBe text("12", "")
        }
    }

    context("anchors") {
        test("anchors a node that is used twice") {
            val shared = yamlMapping { "a" to 1 }
            val node = yamlMapping {
                "first" to shared
                "second" to shared
            }

            yaml.emit(node) shouldBe text("first: &id001", "  a: 1", "second: *id001", "")
        }

        test("keeps the anchor name of a parsed node") {
            val written = yaml.emit(yaml.root(text("base: &base {a: 1}", "copy: *base")))

            written shouldBe text("base: &base {a: 1}", "copy: *base", "")
        }

        test("writes an alias as a key with a space before its colon") {
            val written = yaml.emit(yaml.root(text("- &k key", "- {*k : v}")))

            written shouldContain "*k : v"
        }

        test("anchors a shared node inside a sequence") {
            val shared = yamlSequence { add("x") }
            val written = yaml.emit(
                yamlSequence {
                    add(shared)
                    add(shared)
                },
            )

            yaml.root(written) shouldBe yamlSequence {
                add(shared)
                add(shared)
            }
            written shouldContain "&id001"
        }
    }

    context("documents") {
        test("separates documents with markers") {
            val stream = YamlStream(listOf(YamlDocument(yamlScalar("a")), YamlDocument(yamlScalar("b"))))

            yaml.emit(stream) shouldBe text("a", SECOND_DOCUMENT, "")
        }

        test("starts every document explicitly when asked to") {
            val explicit = yaml { emitter { isDocumentStartExplicit = true } }

            explicit.emit(yamlMapping { "a" to 1 }) shouldBe text("---", "a: 1", "")
        }

        test("writes directives and the end markers they need") {
            val source = text(YAML_DIRECTIVE, "--- a", "...", YAML_DIRECTIVE, SECOND_DOCUMENT)

            yaml.emit(
                yaml.stream(source),
            ) shouldBe text(YAML_DIRECTIVE, "--- a", "...", YAML_DIRECTIVE, SECOND_DOCUMENT, "")
        }

        test("writes a root block scalar after a document marker") {
            val written = yaml.emit(yaml.stream(text("--- |", "  text")))

            written shouldBe text("--- |", "  text", "")
        }

        test("writes a root collection with properties") {
            val written = yaml.emit(yaml.root(text("&root", "a: 1")))

            yaml.root(written) shouldBe yaml.root("a: 1")
            written shouldNotContain "&root"
        }

        test("writes a root collection with a tag") {
            val written = yaml.emit(yaml.root(text("!map", "a: 1")))

            written shouldBe text("!map", "a: 1", "")
        }

        test("writes the root of an explicit document with a tag") {
            yaml.emit(yaml.stream(text("--- !map", "a: 1"))) shouldBe text("--- !map", "a: 1", "")
        }
    }

    context("events") {
        test("writes the events it reads back") {
            val source = text(YAML_DIRECTIVE, "--- !!map &m", "a: [&x b, *x]", "? c", ": 'd'", "...")
            val events = yaml.events(YamlSource.of(source)).events
            val written = yaml.emitEvents(events)

            yaml.events(YamlSource.of(written)).events.map { it::class } shouldBe events.map { it::class }
        }

        test("writes a quoted string that would read as another type") {
            val events = yaml.events(YamlSource.of("['12', \"true\", plain]")).events

            yaml.emitEvents(events) shouldBe text("['12', \"true\", plain]", "")
        }

        test("writes an empty document") {
            yaml.emitEvents(yaml.events(YamlSource.of("---")).events) shouldBe text("--- null", "")
        }

        test("writes a compact mapping of events in a sequence") {
            val source = text("- a: 1", "  b: 2", "- [c]")

            yaml.emitEvents(yaml.events(YamlSource.of(source)).events) shouldBe text(source, "")
        }
    }

    context("options") {
        test("are closed once they are built") {
            var captured: YamlEmitOptionsBuilder? = null
            yaml { emitter { captured = this } }
            val builder = captured ?: error("The emitter block did not run")

            shouldThrow<IllegalStateException> { builder.indent = WIDE_INDENT }
            shouldThrow<IllegalStateException> { builder.isSequenceIndented = false }
            shouldThrow<IllegalStateException> { builder.lineBreak = YamlLineBreak.CrLf }
            shouldThrow<IllegalStateException> { builder.hasSortedKeys = true }
            shouldThrow<IllegalStateException> { builder.isDocumentStartExplicit = true }
            shouldThrow<IllegalStateException> { builder.quoteStyle = YamlQuoteStyle.Single }
            shouldThrow<IllegalStateException> { builder.compatibility = YamlVersion.V1_1 }
            shouldThrow<IllegalStateException> { builder.isStylePreserved = false }
        }

        test("show every setting") {
            YamlEmitOptions.Default.toString() shouldBe
                "YamlEmitOptions(indent=2, isSequenceIndented=true, lineBreak=Lf, hasSortedKeys=false, " +
                "isDocumentStartExplicit=false, quoteStyle=Double, compatibility=1.2, isStylePreserved=true)"
        }
    }

    context("edge cases") {
        test("sorts keys that are no scalars like an empty text") {
            val sorted = yaml { emitter { hasSortedKeys = true } }
            val node = yamlMapping(
                listOf(
                    YamlEntry(key = yamlScalar("b"), value = yamlScalar("1")),
                    YamlEntry(key = yamlSequence(listOf(yamlScalar("s"))), value = yamlScalar("2")),
                    YamlEntry(key = yamlScalar("a"), value = yamlScalar("3")),
                ),
            )

            sorted.emit(node) shouldBe text("? - s", ": \"2\"", "a: \"3\"", "b: \"1\"", "")
        }

        test("separates the anchor of an empty null from the word null in a flow collection") {
            val empty = (yaml.root("a:") as YamlNode.Mapping).entries.single().value
            val node = yamlSequence(listOf(empty, empty), style = YamlCollectionStyle.Flow)
            val written = yaml.emit(node)

            written shouldBe text("[&id001 null, *id001]", "")
            yaml.root(written) shouldBe node
        }

        test("ends a document before the directives of the next one") {
            val stream = YamlStream(
                listOf(
                    YamlDocument(yamlScalar("a")),
                    YamlDocument(yamlScalar("b"), version = YamlVersion.V1_2),
                ),
            )

            yaml.emit(stream) shouldBe text("a", "...", YAML_DIRECTIVE, SECOND_DOCUMENT, "")
        }

        test("keeps a handle with capital letters") {
            val directive = YamlTagDirective(handle = "!Ex!", prefix = "tag:example.com,2026:")
            val written = yaml.emit(YamlDocument(yamlScalar("x"), tagDirectives = listOf(directive)))

            written shouldBe text("%TAG !Ex! tag:example.com,2026:", "--- x", "")
        }

        test("writes lines of a literal block with the configured line break") {
            val windows = yaml { emitter { lineBreak = YamlLineBreak.CrLf } }
            val value = listOf(" one", "", "two", "").joinToString(separator = "\n")
            val node = yamlMapping(listOf(YamlEntry(key = yamlScalar("lines"), value = yamlScalar(value))))
            val written = windows.emit(node)

            written shouldBe listOf("lines: |2", "   one", "", INDENTED_TWO, "").joinToString(separator = "\r\n")
            yaml.root(written) shouldBe node
        }

        test("writes a long key of a flow mapping as it is") {
            val key = "k".repeat(LONG_KEY)
            val node = yamlMapping(
                listOf(YamlEntry(key = yamlScalar(key), value = yamlScalar("v"))),
                style = YamlCollectionStyle.Flow,
            )

            yaml.root(yaml.emit(node)) shouldBe node
        }

        test("writes an empty null as a key of a block mapping") {
            val empty = (yaml.root("a:") as YamlNode.Mapping).entries.single().value
            val mapping = yamlMapping(listOf(YamlEntry(key = empty, value = yamlScalar("v"))))
            val node = yamlSequence(listOf(mapping, mapping))
            val written = yaml.emit(yamlSequence(listOf(mapping)))

            written shouldBe text("- : v", "")
            yaml.root(written) shouldBe yamlSequence(listOf(mapping))
            yaml.root(yaml.emit(node)) shouldBe node
        }

        test("writes a long key with line breaks as a complex key") {
            val key = "line #\n- item: \n".repeat(LONG_KEY / LINE_PARTS)
            val node = yamlMapping(listOf(YamlEntry(key = yamlScalar(key), value = yamlScalar("v"))))
            val written = yaml.emit(node)

            written.startsWith("? |") shouldBe true
            yaml.root(written) shouldBe node
        }

        test("writes a long tagged key of a block mapping as a complex key") {
            val key = yamlTagged("k".repeat(LONG_KEY), YamlTag("!t"))
            val node = yamlMapping(listOf(YamlEntry(key = key, value = yamlScalar("v"))))
            val written = yaml.emit(node)

            written.startsWith("? !t ") shouldBe true
            yaml.root(written) shouldBe node
        }
    }
})
