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

package com.davils.kaml.document.redact

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.yaml
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.YamlDocumentViolationCodes
import com.davils.kaml.document.fixture.lines
import com.davils.kaml.document.fixture.merging
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.fixture.requireConsistent
import com.davils.kaml.document.fixture.treeOf
import com.davils.kaml.document.pointer.at
import com.davils.kaml.document.pointer.toJsonPointer
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val TOKEN: String = "TOKEN-123456"
private const val MARKER: String = "S3CR3T"
private const val NESTING: Int = 8
private const val ENTRIES: Int = 4
private const val MASK: String = "***"
private const val ROOT_PASSWORD: String = "$.password"
private const val INDENTED_MASK: String = "  \"***\""
private const val DERIVED: String = "derived:"
private const val MERGED: String = "  <<: *b"
private const val NAMED: String = "  name: n"
private const val SHORT_QUERY: Int = 4
private const val PASSWORDS: String = "$..password"
private const val PASSWORD: String = "password"
private const val TOKEN_KEY: String = "token"
private const val QUOTED: String = "\"***\""
private const val BASE: String = "base: &b"
private const val SECRET_PASSWORD: String = "  password: secret"
private const val REDACTED_PASSWORD: String = "  password: \"***\""

private val redaction = yamlRedaction {
    path(PASSWORDS)
    key { it.endsWith("secret") }
    value(Regex("TOKEN-[0-9]{6}"))
    comment(Regex("pin=\\d+"))
}

private val representable: List<String> =
    HostileFragments.all - listOf(HostileFragments.LONE_HIGH_SURROGATE, HostileFragments.LONE_LOW_SURROGATE)

private val secrets: Arb<String> = Arb.list(Arb.element(representable), 0..16).map { pieces ->
    MARKER + pieces.joinToString("") + MARKER
}

private fun YamlRedactionResult.text(): String {
    val redacted = shouldBeInstanceOf<YamlRedactionResult.Redacted>()
    return redacted.tree.text
}

private fun redactedText(redaction: YamlRedaction, tree: YamlSyntaxTree): String {
    val result = redaction.redact(tree)
    return result.text()
}

private fun refusalOf(result: YamlRedactionResult): YamlRedactionRefusal =
    result.shouldBeInstanceOf<YamlRedactionResult.Refused>().reason

private fun nested(depth: Int): YamlNode =
    (1..depth).fold<Int, YamlNode>(yamlScalar("x")) { node, _ -> yamlSequence(listOf(node)) }

class YamlRedactionTest : FunSpec({
    context("the graph") {
        test("replaces selected values, keyed values and matching text") {
            val node = nodeOf(
                "{db: {password: hunter2, user: admin}, api_secret: {a: 1}, note: 'key $TOKEN here', port: 1}",
            )

            redaction.redact(node) shouldBe
                nodeOf("{db: {password: '***', user: admin}, api_secret: '***', note: 'key *** here', port: 1}")
        }

        test("redacts items of sequences and the whole document when the root is selected") {
            redaction.redact(nodeOf("[{password: x}, $TOKEN]")) shouldBe nodeOf("[{password: '***'}, '***']")
            yamlRedaction { path("$") }.redact(nodeOf("{a: 1}")) shouldBe nodeOf("'***'")
        }

        test("redacts every alias of a secret node") {
            val node = nodeOf("{base: &b {password: x, user: u}, copy: *b, list: [*b]}")
            val redacted = yamlRedaction { path("$.copy.password") }.redact(node)

            redacted.at("/base/password".toJsonPointer()) shouldBe yamlScalar(MASK)
            redacted.at("/copy/password".toJsonPointer()) shouldBe yamlScalar(MASK)
            redacted.at("/list/0/password".toJsonPointer()) shouldBe yamlScalar(MASK)
            redacted.at("/copy".toJsonPointer()).shouldBeSameInstanceAs(redacted.at("/base".toJsonPointer()))
        }

        test("redacts the source of a member merged through a merge key") {
            val node = nodeOf("{base: &b {password: x}, derived: {<<: *b, name: other}}", merging)
            val redacted = yamlRedaction { path("$.derived.password") }.redact(node)

            redacted.at("/base/password".toJsonPointer()) shouldBe yamlScalar(MASK)
            redacted.at("/derived/name".toJsonPointer()) shouldBe yamlScalar("other")
        }

        test("redacts every key with the same text") {
            val sameText = yamlMapping(
                listOf(
                    YamlEntry(key = yamlScalar(1), value = yamlScalar("a")),
                    YamlEntry(key = yamlScalar("1"), value = yamlScalar("b")),
                    YamlEntry(key = yamlScalar(2), value = yamlScalar("c")),
                    YamlEntry(key = yamlScalar("2"), value = yamlScalar("d")),
                ),
            )
            val redacted = yamlRedaction {
                path("$['1']")
                key { it == "2" }
            }.redact(sameText) as YamlNode.Mapping

            redacted.entries.map { entry -> entry.value } shouldBe List(ENTRIES) { yamlScalar(MASK) }
        }

        test("refuses to make two keys of a mapping equal") {
            val keys = nodeOf("{[{p: x}]: 1, [{p: y}]: 2}")
            val error = shouldThrow<IllegalStateException> { yamlRedaction { key { it == "p" } }.redact(keys) }

            error.message shouldBe "The redacted graph would hold a key of a mapping twice"
        }

        test("keeps a node that holds no secret") {
            val node = nodeOf("{a: [1, 2], b: {c: d}}")

            redaction.redact(node).shouldBeSameInstanceAs(node)
        }

        test("redacts inside keys that are collections, but never applies value patterns to keys") {
            val node = nodeOf("{{password: x}: 1, $TOKEN: 2}")
            val keyed = yamlRedaction {
                key { it == "password" }
                value(Regex("TOKEN-[0-9]{6}"))
            }
            val redacted = keyed.redact(node) as YamlNode.Mapping

            redacted.entries.first().key shouldBe nodeOf("{password: '***'}")
            redacted.entries.last().key shouldBe yamlScalar(TOKEN)
        }

        test("writes the replacement literally") {
            val literal = yamlRedaction {
                value(Regex("TOKEN-([0-9]+)"))
                replacement = "$1\\0"
            }

            literal.redact(nodeOf("'a $TOKEN'")) shouldBe yamlScalar("a $1\\0")
        }

        test("hands a value pattern no more text than its limit allows") {
            val bounded = yamlRedaction {
                value(Regex("x"))
                limits = yamlLimits { maximumPatternInputLength = 4 }
            }

            bounded.redact(yamlScalar("abcx")) shouldBe yamlScalar("abc***")
            shouldThrow<YamlDocumentLimitException> { bounded.redact(yamlScalar("abcdx")) }.limit shouldBe
                "maximumPatternInputLength"
        }

        test("refuses a graph deeper than its limit") {
            val shallow = yamlRedaction {
                key { false }
                limits = yamlLimits { maximumDepth = NESTING }
            }

            shallow.redact(nested(NESTING)) shouldBe nested(NESTING)
            shouldThrow<YamlDocumentLimitException> { shallow.redact(nested(NESTING + 1)) }.limit shouldBe
                "maximumDepth"
        }

        test("fails without the key when a key predicate throws") {
            val failing = yamlRedaction { key { key -> error("bad key $key") } }
            val error = shouldThrow<IllegalStateException> { failing.redact(nodeOf("{$MARKER: 1}")) }

            error.message shouldBe "A key predicate of the redaction failed"
            error.cause.shouldBeNull()
        }

        test("rethrows an error of a key predicate as it is") {
            val failing = yamlRedaction { key { throw AssertionError("stop") } }

            shouldThrow<AssertionError> { failing.redact(nodeOf("{a: 1}")) }
        }
    }

    context("the syntax tree") {
        val text = lines(
            "# deploy pin=1234",
            "db:",
            "  user: admin",
            "  password: hunter2 # rotate yearly",
            "api_secret:",
            "  a: 1",
            "  b: &k [x]",
            "token: |",
            "  $TOKEN",
            "flow: {password: 'p', other: \"$TOKEN\"}",
            "alias: *k",
        )

        test("replaces values in place, with their trailing comments and the aliases of removed anchors") {
            val redacted = redaction.redact(treeOf(text)).shouldBeInstanceOf<YamlRedactionResult.Redacted>()

            requireConsistent(text, redacted.edits, redacted.tree.text)
            redacted.tree.text shouldBe lines(
                "# deploy ***",
                "db:",
                "  user: admin",
                "  password: \"***\" # ***",
                "api_secret:",
                INDENTED_MASK,
                """token: "***\n"""",
                """flow: {password: "***", other: "***"}""",
                "alias: \"***\"",
            )
            redacted.toString() shouldBe "Redacted(${redacted.edits.size} edits)"
        }

        test("keeps trailing comments when told to") {
            val keeping = yamlRedaction {
                path(ROOT_PASSWORD)
                isTrailingCommentRedacted = false
            }

            keeping.redact(treeOf("password: x # note")).text() shouldBe "password: \"***\" # note"
        }

        test("redacts every document and escapes the replacement") {
            val quoting = yamlRedaction {
                key { it == "a" }
                replacement = """say "no" \"""
            }

            quoting.redact(treeOf(lines("a: 1", "---", "a: 2"))).text() shouldBe
                lines("""a: "say \"no\" \\"""", "---", """a: "say \"no\" \\"""")
        }

        test("escapes control and separator characters") {
            val control = yamlRedaction {
                key { true }
                replacement = Char(1).toString() + "\u0085\u2028"
            }

            control.redact(treeOf("a: 1")).text() shouldBe """a: "\x01\x85\u2028""""
        }

        test("refuses a broken document instead of redacting it in part") {
            refusalOf(redaction.redact(treeOf(lines("password: x", "b: [1")))) shouldBe
                YamlRedactionRefusal.InvalidDocument
        }

        test("redacts the anchored source of a target reached through an alias") {
            val tree = treeOf(lines(BASE, SECRET_PASSWORD, "copy: *b"))

            redactedText(yamlRedaction { path("$.copy.password") }, tree) shouldBe
                lines(BASE, REDACTED_PASSWORD, "copy: *b")
        }

        test("redacts the source of a target merged through a merge key") {
            val tree = treeOf(lines(BASE, SECRET_PASSWORD, DERIVED, MERGED, NAMED), merging)

            redactedText(yamlRedaction { path("$.derived.password") }, tree) shouldBe
                lines(BASE, REDACTED_PASSWORD, DERIVED, MERGED, NAMED)
        }

        test("redacts what an alias inside a target stands for") {
            val tree = treeOf(lines("token: &t secret", "config:", "  value: *t"))

            redactedText(yamlRedaction { path("$.config") }, tree) shouldBe
                lines("token: &t \"***\"", "config:", "  \"***\"")
        }

        test("empties a merge key whose anchor went with a redacted value") {
            val tree = treeOf(lines("secrets:", "  base: &b {password: x}", "app:", MERGED, NAMED), merging)

            redactedText(yamlRedaction { path("$.secrets") }, tree) shouldBe
                lines("secrets:", INDENTED_MASK, "app:", "  <<: {}", NAMED)
        }

        test("redacts every occurrence of a duplicate key the graph dropped") {
            val keepFirst = yaml { duplicateKeys = YamlDuplicateKeys.KeepFirst }
            val text = lines("password: a", "password: b", "token: c", "token: d", "other: e")
            val tree = YamlSyntaxTree.parse(text, keepFirst)
            val duplicates = yamlRedaction {
                path(ROOT_PASSWORD)
                key { it == TOKEN_KEY }
            }

            duplicates.redact(tree).text() shouldBe lines(
                "password: \"***\"",
                "password: \"***\"",
                "token: \"***\"",
                "token: \"***\"",
                "other: e",
            )
        }

        test("writes the replacement literally") {
            val literal = yamlRedaction {
                value(Regex("TOKEN"))
                comment(Regex("pin"))
                replacement = "$0"
            }

            literal.redact(treeOf("a: $TOKEN # pin")).text() shouldBe "a: \"$0-123456\" # $0"
        }

        test("refuses a scalar or a comment longer than a pattern may read") {
            val bounded = yamlRedaction {
                value(Regex("x"))
                comment(Regex("y"))
                limits = yamlLimits { maximumPatternInputLength = 4 }
            }

            bounded.redact(treeOf("a: abcx # y")).text() shouldBe "a: \"abc***\" # ***"
            refusalOf(bounded.redact(treeOf("a: abcdx"))) shouldBe YamlRedactionRefusal.LimitExceeded
            refusalOf(bounded.redact(treeOf("a: b # abcdy"))) shouldBe YamlRedactionRefusal.LimitExceeded
        }

        test("refuses a document whose queries exceed their budget") {
            val costly = yamlRedaction {
                path("$..*")
                limits = yamlLimits { maximumQuerySteps = 10 }
            }

            refusalOf(costly.redact(treeOf("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"))) shouldBe
                YamlRedactionRefusal.LimitExceeded
        }

        test("removes the tag of a redacted value, so the replacement still composes") {
            val tagged = yamlRedaction { key { it == "a" || it == "b" } }

            tagged.redact(treeOf(lines("a: !!int 1", "b: &x !!map", "  c: d", "e: *x"))).text() shouldBe
                lines("a:  \"***\"", "b: &x ", INDENTED_MASK, "e: *x")
        }

        test("refuses a redaction whose result would not compose") {
            val strict = yaml { limits { maximumScalarLength = 4 } }
            val long = yamlRedaction {
                key { true }
                replacement = "0123456789"
            }

            refusalOf(long.redact(treeOf("a: 1", strict))) shouldBe YamlRedactionRefusal.Undecidable
            YamlRedactionResult.Refused(YamlRedactionRefusal.Undecidable).toString() shouldBe "Refused(Undecidable)"
        }
    }

    context("secrets") {
        test("never appear in the redacted graph, text or descriptions") {
            val bySecret = yamlRedaction { path(PASSWORDS) }
            checkAll(secrets) { secret ->
                val node = yamlMapping(listOf(YamlEntry(key = yamlScalar(PASSWORD), value = yamlScalar(secret))))
                val text = Yaml.Default.emit(node)
                val graph = bySecret.redact(node)
                val result = bySecret.redact(treeOf(text))

                Yaml.Default.emit(graph) shouldNotContain MARKER
                result.text() shouldNotContain MARKER
                result.toString() shouldNotContain MARKER
                bySecret.toString() shouldNotContain MARKER
            }
        }

        test("never appear in the failure of a key predicate") {
            val failing = yamlRedaction { key { key -> error(key) } }
            checkAll(secrets) { secret ->
                val node = yamlMapping(listOf(YamlEntry(key = yamlScalar(secret), value = yamlScalar(1))))
                val error = shouldThrow<IllegalStateException> { failing.redact(node) }

                error.message.orEmpty() shouldNotContain MARKER
            }
        }
    }

    context("configuration") {
        test("reports invalid paths and replacements") {
            val error = shouldThrow<ValidationException> {
                yamlRedaction {
                    path("$.")
                    path("$.a")
                    replacement = "a\nb"
                }
            }

            error.report.violations.map { it.code } shouldBe listOf(
                YamlDocumentViolationCodes.INVALID_PATH,
                YamlDocumentViolationCodes.INVALID_REPLACEMENT,
            )
        }

        test("refuses a path beyond its limits") {
            shouldThrow<ValidationException> {
                yamlRedaction {
                    path("$.abcdef")
                    limits = yamlLimits { maximumQueryLength = SHORT_QUERY }
                }
            }
        }

        test("describes itself with escaped text") {
            redaction.paths shouldBe listOf(PASSWORDS)
            redaction.replacement shouldBe MASK
            redaction.isTrailingCommentRedacted shouldBe true
            redaction.toString() shouldBe """YamlRedaction(replacement="***", paths=["$PASSWORDS"])"""
            checkAll(secrets) { hostile ->
                val shown = yamlRedaction { replacement = hostile.filter { it != '\n' && it != '\r' } }
                unsafeCodePointsIn(shown.toString()).shouldBeEmpty()
            }
        }

        test("refuses a carriage return in the replacement and changes after it was built") {
            shouldThrow<ValidationException> { yamlRedaction { replacement = "a\rb" } }
            val builder = YamlRedactionBuilder()
            builder.build()

            shouldThrow<IllegalStateException> { builder.path("$") }
        }
    }

    context("edges") {
        test("redacts nested targets once and skips empty values") {
            val nested = yamlRedaction { key { it == "outer" || it == "inner" || it == "e" } }
            val text = lines("outer:", "  inner: x", "e:", "? k")

            nested.redact(treeOf(text)).text() shouldBe lines("outer:", INDENTED_MASK, "e:", "? k")
        }

        test("names members with complex keys by their kind") {
            val complex = yamlRedaction { key { it == "Sequence" } }

            complex.redact(nodeOf("{[a]: 1}")) shouldBe nodeOf("{[a]: '***'}")
            complex.redact(treeOf(lines("? [a]", ": 1"))).text() shouldBe lines("? [a]", ": $QUOTED")
        }

        test("names members with keys that are mappings or aliases") {
            val mappingKey = yamlRedaction { key { it == "Mapping" } }
            val aliasKey = yamlRedaction { key { it == "secret" } }

            redactedText(mappingKey, treeOf(lines("? {a: 1}", ": x"))) shouldBe lines("? {a: 1}", ": $QUOTED")
            redactedText(
                aliasKey,
                treeOf(lines("k: &k secret", "*k : v")),
            ) shouldBe lines("k: &k secret", "*k : $QUOTED")
        }

        test("leaves empty values and unique duplicates alone") {
            val keepFirst = yaml { duplicateKeys = YamlDuplicateKeys.KeepFirst }
            val tree = YamlSyntaxTree.parse(lines("a: 1", "a: 2", "password:"), keepFirst)

            redactedText(yamlRedaction { key { it == PASSWORD } }, tree) shouldBe lines("a: 1", "a: 2", "password:")
        }

        test("replaces orphaned aliases in sequences and keys and skips comments inside a target") {
            val items = treeOf(lines("- {a: &k 1}", "- *k"))
            val keys = treeOf(lines("a: {x: &k 1}", "? *k", ": v"))
            val commented = treeOf(lines("a:", "  b: 1", "  # inner", "  d: 3", "c: 2"))

            redactedText(yamlRedaction { path("$[0]") }, items) shouldBe lines("- \"***\"", "- \"***\"")
            redactedText(yamlRedaction { path("$.a") }, keys) shouldBe lines("a: $QUOTED", "? $QUOTED", ": v")
            redactedText(yamlRedaction { key { it == "a" } }, commented) shouldBe lines("a:", INDENTED_MASK, "c: 2")
        }

        test("refuses a syntax tree deeper than its limits") {
            val shallow = yamlRedaction {
                key { false }
                limits = yamlLimits { maximumDepth = 2 }
            }

            refusalOf(shallow.redact(treeOf("a: [[[1]]]"))) shouldBe YamlRedactionRefusal.LimitExceeded
        }

        test("compares refusals by their reason") {
            val refused = YamlRedactionResult.Refused(YamlRedactionRefusal.InvalidDocument)

            refused shouldBe YamlRedactionResult.Refused(YamlRedactionRefusal.InvalidDocument)
            refused.hashCode() shouldBe YamlRedactionRefusal.InvalidDocument.hashCode()
            refused.equals(YamlRedactionResult.Refused(YamlRedactionRefusal.LimitExceeded)) shouldBe false
            refused.equals(YamlRedactionRefusal.InvalidDocument) shouldBe false
        }
    }
})
