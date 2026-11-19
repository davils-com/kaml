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
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.stream
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.node.yamlTagged
import com.davils.kore.core.text.TextRange
import com.davils.kore.testing.HostileFragments
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val TAG_ITERATIONS: Int = 300
private const val LONGEST_TAG: Int = 10
private const val GLOBAL_PREFIX: String = "tag:example.com,2026:"
private const val URI_CHARACTERS: String =
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-#;/?:@&=+$,_.!~*'()[]%"
private const val GENERATED_ANCHOR: String = "id001"

private val tagFragments: List<String> = HostileFragments.all +
    listOf(
        ">", "<", "!", "!!", "!<", "%", "%2", "%41", "%zz", " ", "\n", "\n--- !x\n", ",", "[", "]", "{", "}", "#", " #",
        "&a", "*a", "'", "ä", ":", ": ", "a", "-", "\u0085", "\u2028", "\uDBFF\uDFFF",
    )

private val tagText: Arb<String> = Arb.list(Arb.element(tagFragments), 0..LONGEST_TAG).map { parts ->
    parts.joinToString(separator = "")
}

private val foreignAnchorNames: List<String> = listOf("a b", "a,b", "a]b", "a\u202Eb", "ä", "a:b", "a/b", "a*b")

private val range: TextRange = TextRange.at(0)

class YamlEmitterInjectionTest : FunSpec({
    val yaml = Yaml.Default

    context("tags") {
        test("a hostile local or core tag reads back as the same tag") {
            checkAll(TAG_ITERATIONS, tagText) { suffix ->
                listOfNotNull(
                    tagOrNull("!t$suffix"),
                    tagOrNull(YamlTag.CORE_TAG_PREFIX + "t" + suffix),
                ).forEach { tag ->
                    shouldReadBack(yaml, tag)
                }
            }
        }

        test("a hostile global tag is percent-encoded and cannot break the output") {
            checkAll(TAG_ITERATIONS, tagText) { suffix ->
                tagOrNull(GLOBAL_PREFIX + suffix)?.let { tag -> shouldStayContained(yaml, tag) }
            }
        }

        test("a global tag of URI characters reads back as it is") {
            val tag = YamlTag("${GLOBAL_PREFIX}a%41[b]")

            yaml.emit(yamlTagged("x", tag)) shouldBe text("!<${GLOBAL_PREFIX}a%41[b]> x", "")
        }

        test("encodes characters of a global tag that are not allowed in a URI") {
            val tag = YamlTag("${GLOBAL_PREFIX}a b>\u00E4%zz")

            yaml.emit(yamlTagged("x", tag)) shouldBe text("!<${GLOBAL_PREFIX}a%20b%3E%C3%A4%25zz> x", "")
        }

        test("writes the non-specific tag and an empty core suffix") {
            val node = yamlSequence(
                listOf(
                    yamlTagged("x", YamlTag.NON_SPECIFIC),
                    yamlTagged("y", YamlTag(YamlTag.CORE_TAG_PREFIX)),
                ),
            )

            yaml.emit(node) shouldBe text("- ! x", "- !<tag:yaml.org,2002:> y", "")
        }

        test("writes a tag of a key with an empty body before a separated colon") {
            val tag = YamlTag("!t:")
            val node = yamlMapping(listOf(YamlEntry(key = yamlTagged("", tag), value = yamlTagged("", tag))))
            val written = yaml.emit(node)

            written shouldBe text("!t: : !t:", "")
            yaml.root(written) shouldBe node
        }
    }

    context("tag directives") {
        test("write a local tag verbatim when the primary handle is redefined") {
            val directive = YamlTagDirective(handle = "!", prefix = GLOBAL_PREFIX)
            val document = YamlDocument(root = yamlTagged("x", YamlTag("!local")), tagDirectives = listOf(directive))
            val written = yaml.emit(document)

            written shouldBe text("%TAG ! $GLOBAL_PREFIX", "--- !<!local> x", "")
            yaml.root(written) shouldBe yamlTagged("x", YamlTag("!local"))
        }

        test("write a core tag verbatim when the secondary handle is redefined") {
            val directive = YamlTagDirective(handle = "!!", prefix = GLOBAL_PREFIX)
            val tag = YamlTag.core("custom")
            val written = yaml.emit(YamlDocument(root = yamlTagged("x", tag), tagDirectives = listOf(directive)))

            written shouldBe text("%TAG !! $GLOBAL_PREFIX", "--- !<tag:yaml.org,2002:custom> x", "")
            yaml.root(written) shouldBe yamlTagged("x", tag)
        }

        test("keep a named handle and a local prefix") {
            val directive = YamlTagDirective(handle = "!e-1!", prefix = "!local-")
            val written = yaml.emit(YamlDocument(root = yamlScalar("x"), tagDirectives = listOf(directive)))

            written shouldBe text("%TAG !e-1! !local-", "--- x", "")
        }

        test("refuse a handle that is not one") {
            listOf("", "!e", "e!", "!é!", "!!!", "!a b!", "!\n!").forEach { handle ->
                val document = YamlDocument(yamlScalar("x"), tagDirectives = listOf(YamlTagDirective(handle, "x")))

                shouldThrow<IllegalArgumentException> {
                    yaml.emit(
                        document,
                    )
                }.message shouldBe TagNotation.INVALID_HANDLE
            }
        }

        test("refuse an empty prefix and a handle declared twice") {
            val empty = listOf(YamlTagDirective(handle = "!e!", prefix = ""))
            val twice = listOf(
                YamlTagDirective(handle = "!e!", prefix = "a"),
                YamlTagDirective(handle = "!e!", prefix = "b"),
            )

            shouldThrow<IllegalArgumentException> {
                yaml.emit(YamlDocument(yamlScalar("x"), tagDirectives = empty))
            }.message shouldBe TagNotation.MISSING_PREFIX
            shouldThrow<IllegalArgumentException> {
                yaml.emit(YamlDocument(yamlScalar("x"), tagDirectives = twice))
            }.message shouldBe TagNotation.REPEATED_HANDLE
        }

        test("percent-encode a hostile prefix so that it cannot start a node or a document") {
            checkAll(TAG_ITERATIONS, tagText) { suffix ->
                shouldEncodePrefix(yaml, GLOBAL_PREFIX + suffix)
                shouldEncodePrefix(yaml, "!$suffix")
            }
        }

        test("percent-encode a flow indicator that starts a prefix") {
            val directive = YamlTagDirective(handle = "!e!", prefix = "[a,b]")
            val written = yaml.emit(YamlDocument(root = yamlScalar("x"), tagDirectives = listOf(directive)))

            written shouldBe text("%TAG !e! %5Ba,b]", "--- x", "")
        }
    }

    context("anchors") {
        test("keep a valid anchor name and replace any other by a generated one") {
            checkAll(TAG_ITERATIONS, tagText) { name ->
                anchorOrNull("a$name")?.let { anchor -> shouldNameAnchor(yaml, anchor) }
            }
        }

        test("replace a foreign anchor of an event and the aliases that refer to it") {
            firstForeignAnchor()?.let { foreign -> shouldReplaceForeignAnchor(yaml, foreign) }
        }

        test("keep the name of an anchor that a later document defines again") {
            val anchor = YamlAnchor("a")
            val document = listOf(
                YamlEvent.DocumentStart(range = range, isExplicit = true, version = null, tagDirectives = emptyList()),
                scalarEvent("x", anchor),
                YamlEvent.DocumentEnd(range = range, isExplicit = false),
            )

            yaml.emitEvents(document + document) shouldBe text("--- &a x", "--- &a x", "")
        }
    }
})

private fun tagOrNull(uri: String): YamlTag? = try {
    YamlTag(uri)
} catch (_: IllegalArgumentException) {
    null
}

private fun anchorOrNull(name: String): YamlAnchor? = try {
    YamlAnchor(name)
} catch (_: IllegalArgumentException) {
    null
}

private fun firstForeignAnchor(): YamlAnchor? =
    foreignAnchorNames.firstNotNullOfOrNull(::anchorOrNull)?.takeIf { anchor -> !AnchorNames.isValid(anchor.name) }

private fun taggedGraph(tag: YamlTag): YamlNode = yamlSequence(
    listOf(
        yamlTagged("x", tag),
        yamlMapping(listOf(YamlEntry(key = yamlTagged("k", tag), value = yamlTagged("", tag)))),
        yamlMapping(listOf(YamlEntry(key = yamlTagged("", tag), value = yamlTagged("v", tag)))),
        yamlSequence(listOf(yamlTagged("y", tag), yamlTagged("", tag)), style = YamlCollectionStyle.Flow),
        yamlSequence(listOf(yamlScalar("z")), tag = tag),
        yamlMapping(listOf(YamlEntry(key = yamlScalar("k"), value = yamlScalar("v"))), tag = tag),
    ),
)

private fun shouldReadBack(yaml: Yaml, tag: YamlTag) {
    val node = taggedGraph(tag)
    if (UnsafeCharacters.hasLoneSurrogate(tag.uri)) {
        shouldThrow<IllegalArgumentException> { yaml.emit(node) }.message shouldBe UriText.LONE_SURROGATE
        return
    }
    val written = yaml.emit(node)
    withClue(written) { yaml.root(written) shouldBe node }
}

private fun shouldStayContained(yaml: Yaml, tag: YamlTag) {
    val node = taggedGraph(tag)
    if (UnsafeCharacters.hasLoneSurrogate(tag.uri)) {
        shouldThrow<IllegalArgumentException> { yaml.emit(node) }.message shouldBe UriText.LONE_SURROGATE
        return
    }
    val written = yaml.emit(node)
    val encoded = UriText.verbatim(tag.uri)
    withClue(written) {
        encoded.all { character -> character in URI_CHARACTERS } shouldBe true
        yaml.root(written) shouldBe taggedGraph(YamlTag(encoded))
    }
}

private fun shouldEncodePrefix(yaml: Yaml, prefix: String) {
    val document = YamlDocument(
        root = yamlScalar("x"),
        tagDirectives = listOf(YamlTagDirective(handle = "!e!", prefix = prefix)),
    )
    if (UnsafeCharacters.hasLoneSurrogate(prefix)) {
        shouldThrow<IllegalArgumentException> { yaml.emit(document) }.message shouldBe UriText.LONE_SURROGATE
        return
    }
    val written = yaml.emit(document)
    val encoded = UriText.prefix(prefix)
    withClue(written) {
        encoded.all { character -> character in URI_CHARACTERS } shouldBe true
        val read = yaml.stream(written).documents.single()
        read.tagDirectives shouldBe listOf(YamlTagDirective(handle = "!e!", prefix = encoded))
        read.root shouldBe yamlScalar("x")
    }
}

private fun shouldNameAnchor(yaml: Yaml, anchor: YamlAnchor) {
    val shared = yamlSequence(listOf(yamlScalar("x")), anchor = anchor)
    val node = yamlSequence(listOf(shared, shared))
    val written = yaml.emit(node)
    val expected = anchor.name.takeIf(AnchorNames::isValid) ?: GENERATED_ANCHOR
    withClue(written) {
        written shouldBe text("- &$expected", "  - x", "- *$expected", "")
        yaml.root(written) shouldBe node
    }
}

private fun shouldReplaceForeignAnchor(yaml: Yaml, foreign: YamlAnchor) {
    val events = stream(
        YamlEvent.SequenceStart(range = range, anchor = null, tag = null, style = YamlCollectionStyle.Block),
        scalarEvent("x", foreign),
        scalarEvent("y", YamlAnchor(GENERATED_ANCHOR)),
        YamlEvent.Alias(range = range, anchor = foreign),
        YamlEvent.Alias(range = range, anchor = YamlAnchor(GENERATED_ANCHOR)),
        YamlEvent.SequenceEnd(range = range),
    )
    val written = yaml.emitEvents(events)

    written shouldBe text("- &id002 x", "- &id001 y", "- *id002", "- *id001", "")
    written shouldNotContain foreign.name
}

private fun scalarEvent(value: String, anchor: YamlAnchor?): YamlEvent.Scalar =
    YamlEvent.Scalar(range = range, anchor = anchor, tag = null, value = value, style = YamlScalarStyle.Plain)

private fun stream(vararg nodes: YamlEvent): List<YamlEvent> = buildList {
    add(YamlEvent.StreamStart(range = range))
    add(YamlEvent.DocumentStart(range = range, isExplicit = false, version = null, tagDirectives = emptyList()))
    addAll(nodes)
    add(YamlEvent.DocumentEnd(range = range, isExplicit = false))
    add(YamlEvent.StreamEnd(range = range))
}
