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
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.yaml
import com.davils.kore.core.text.TextRange
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val SHORT_OUTPUT: Int = 16
private const val LONG_TEXT: Int = 64
private const val READABLE_DEPTH: Int = 64
private const val SHALLOW_DEPTH: Int = 8

private val range: TextRange = TextRange.at(0)
private val streamStart: YamlEvent = YamlEvent.StreamStart(range = range)
private val streamEnd: YamlEvent = YamlEvent.StreamEnd(range = range)
private val documentStart: YamlEvent =
    YamlEvent.DocumentStart(range = range, isExplicit = false, version = null, tagDirectives = emptyList())
private val documentEnd: YamlEvent = YamlEvent.DocumentEnd(range = range, isExplicit = false)
private val sequenceStart: YamlEvent =
    YamlEvent.SequenceStart(range = range, anchor = null, tag = null, style = YamlCollectionStyle.Block)
private val sequenceEnd: YamlEvent = YamlEvent.SequenceEnd(range = range)
private val mappingStart: YamlEvent =
    YamlEvent.MappingStart(range = range, anchor = null, tag = null, style = YamlCollectionStyle.Block)
private val mappingEnd: YamlEvent = YamlEvent.MappingEnd(range = range)
private val anchored: YamlEvent = scalarEvent("x", YamlAnchor("a"))
private val alias: YamlEvent = YamlEvent.Alias(range = range, anchor = YamlAnchor("a"))

class YamlEmitterLimitsTest : FunSpec({
    val yaml = Yaml.Default

    context("the output length") {
        val node = yamlMapping(listOf(YamlEntry(key = yamlScalar("key"), value = yamlScalar("value"))))

        test("may reach the maximum output length") {
            val written = yaml.emit(node)

            bounded(written.length).emit(node) shouldBe written
        }

        test("stops one character past the maximum output length") {
            val written = yaml.emit(node)

            shouldThrow<IllegalStateException> {
                bounded(written.length - 1).emit(node)
            }.message shouldBe OutputBuffer.TOO_LONG
        }

        test("stops inside every part of a document") {
            val long = "x".repeat(LONG_TEXT)
            val parts = listOf(
                yamlScalar("$long\n$long\n"),
                yamlScalar("\u0000$long"),
                yamlScalar("'$long", YamlScalarStyle.SingleQuoted),
                yamlSequence(listOf(yamlScalar(long)), style = YamlCollectionStyle.Flow),
                yamlMapping(listOf(YamlEntry(key = yamlScalar(long), value = yamlScalar("v")))),
                yamlSequence(listOf(yamlSequence(listOf(yamlScalar("a"), yamlScalar(long))))),
                yamlMapping(listOf(YamlEntry(key = yamlScalar("k"), value = yamlMapping { long to long }))),
            )

            parts.forEach { part ->
                shouldThrow<IllegalStateException> { bounded(SHORT_OUTPUT).emit(part) }.message shouldBe
                    OutputBuffer.TOO_LONG
            }
        }

        test("stops inside directives, markers and comments") {
            val long = "x".repeat(LONG_TEXT)
            val root = yamlScalar("x")
            val directed = YamlDocument(
                root = root,
                version = YamlVersion.V1_2,
                tagDirectives = listOf(YamlTagDirective(handle = "!e!", prefix = long)),
            )
            val commented = yamlComments { comment(root, long) }

            shouldThrow<IllegalStateException> { bounded(SHORT_OUTPUT).emit(directed) }
            shouldThrow<IllegalStateException> { bounded(SHORT_OUTPUT).emit(YamlDocument(root), commented) }
        }

        test("bounds the output of events") {
            val events = listOf(documentStart, scalarEvent("x".repeat(LONG_TEXT), anchor = null), documentEnd)

            shouldThrow<IllegalStateException> {
                bounded(SHORT_OUTPUT).emitEvents(events)
            }.message shouldBe OutputBuffer.TOO_LONG
        }
    }

    context("the depth") {
        val deep = yaml { limits { maximumDepth = YamlLimits.HARD_DEPTH } }
        val shallow = yaml { limits { maximumDepth = SHALLOW_DEPTH } }

        test("lets nodes nest one level less deep than the depth limit in every style") {
            val depth = YamlLimits.HARD_DEPTH - 1
            val keys = (0 until depth - 1).joinToString(separator = "\n") { level -> "  ".repeat(level) + "k:" }
            val brackets = "[".repeat(depth) + "]".repeat(depth)

            deep.emit(nestedSequences(depth, YamlCollectionStyle.Block)) shouldBe "- ".repeat(depth - 1) + "[]\n"
            deep.emit(nestedSequences(depth, YamlCollectionStyle.Flow)) shouldBe "$brackets\n"
            deep.emit(nestedMappings(depth, YamlCollectionStyle.Block)) shouldBe "$keys {}\n"
            deep.emit(nestedMappings(depth, YamlCollectionStyle.Flow)) shouldBe
                "{k: ".repeat(depth - 1) + "{}" + "}".repeat(depth - 1) + "\n"
        }

        test("writes nodes as deep as the configuration allows, which read back as the same graph") {
            YamlCollectionStyle.entries.forEach { style ->
                val nodes = listOf(nestedSequences(SHALLOW_DEPTH - 1, style), nestedMappings(SHALLOW_DEPTH - 1, style))
                nodes.forEach { node -> shallow.root(shallow.emit(node)) shouldBe node }
            }
            deep.root(deep.emit(nestedSequences(READABLE_DEPTH, YamlCollectionStyle.Flow))) shouldBe
                nestedSequences(READABLE_DEPTH, YamlCollectionStyle.Flow)
        }

        test("refuses a node that its parser would refuse for its depth") {
            YamlCollectionStyle.entries.forEach { style ->
                shouldThrow<IllegalArgumentException> { shallow.emit(nestedSequences(SHALLOW_DEPTH, style)) }
                    .message shouldBe Representer.tooDeep(SHALLOW_DEPTH)
                shouldThrow<IllegalArgumentException> { shallow.emit(nestedMappings(SHALLOW_DEPTH, style)) }
                shouldThrow<IllegalArgumentException> { yaml.emit(nestedSequences(YamlLimits.HARD_DEPTH, style)) }
                shouldThrow<IllegalArgumentException> { deep.emit(nestedMappings(YamlLimits.HARD_DEPTH, style)) }
            }
        }

        test("counts the collections of a key toward the depth") {
            val fitting = keyedBy(nestedSequences(SHALLOW_DEPTH - 2, YamlCollectionStyle.Flow))
            val tooDeep = keyedBy(nestedSequences(SHALLOW_DEPTH - 1, YamlCollectionStyle.Flow))

            shallow.root(shallow.emit(fitting)) shouldBe fitting
            shouldThrow<IllegalArgumentException> { shallow.emit(tooDeep) }
        }

        test("lets events nest one level less deep than the depth limit") {
            val depth = YamlLimits.HARD_DEPTH - 1

            deep.emitEvents(nestedEvents(depth)) shouldBe "- ".repeat(depth - 1) + "[]\n"
            shallow.root(shallow.emitEvents(nestedEvents(SHALLOW_DEPTH - 1))) shouldBe
                nestedSequences(SHALLOW_DEPTH - 1, YamlCollectionStyle.Block)
        }

        test("refuses events that its parser would refuse for their depth") {
            shouldThrow<IllegalArgumentException> {
                shallow.emitEvents(nestedEvents(SHALLOW_DEPTH))
            }.message shouldBe EventTree.tooDeep(SHALLOW_DEPTH)
            shouldThrow<IllegalArgumentException> { deep.emitEvents(nestedEvents(YamlLimits.HARD_DEPTH)) }
        }
    }

    context("an event sequence") {
        test("may leave out the stream events and may hold no document at all") {
            yaml.emitEvents(listOf(documentStart, scalarEvent("x", anchor = null), documentEnd)) shouldBe "x\n"
            yaml.emitEvents(listOf(streamStart, streamEnd)) shouldBe ""
            yaml.emitEvents(emptyList()) shouldBe ""
        }

        test("refuses a stream start that is not the first event") {
            shouldRefuse(EventTree.MISPLACED_STREAM_START, streamStart, streamStart)
            shouldRefuse(EventTree.MISPLACED_STREAM_START, documentStart, streamStart)
        }

        test("refuses a stream end inside a document or after the end of the stream") {
            shouldRefuse(EventTree.MISPLACED_STREAM_END, documentStart, streamEnd)
            shouldRefuse(EventTree.MISPLACED_STREAM_END, streamEnd, streamEnd)
        }

        test("refuses a document start inside a document or after the end of the stream") {
            shouldRefuse(EventTree.MISPLACED_DOCUMENT_START, documentStart, documentStart)
            shouldRefuse(EventTree.MISPLACED_DOCUMENT_START, streamEnd, documentStart)
        }

        test("refuses a document end without a document or without a complete root") {
            shouldRefuse(EventTree.MISPLACED_DOCUMENT_END, documentEnd)
            shouldRefuse(EventTree.MISPLACED_DOCUMENT_END, documentStart, documentEnd)
            shouldRefuse(EventTree.MISPLACED_DOCUMENT_END, documentStart, sequenceStart, documentEnd)
        }

        test("refuses a node outside a document and a second root") {
            shouldRefuse(EventTree.MISPLACED_NODE, scalarEvent("x", anchor = null))
            shouldRefuse(EventTree.MISPLACED_NODE, documentStart, anchored, anchored)
            shouldRefuse(EventTree.MISPLACED_NODE, documentStart, anchored, alias)
            shouldRefuse(EventTree.MISPLACED_NODE, documentStart, anchored, sequenceStart)
        }

        test("refuses an end that does not match the open collection") {
            shouldRefuse(EventTree.UNMATCHED_END, documentStart, sequenceEnd)
            shouldRefuse(EventTree.UNMATCHED_END, documentStart, sequenceStart, mappingEnd)
            shouldRefuse(EventTree.UNMATCHED_END, documentStart, mappingStart, sequenceEnd)
        }

        test("refuses a mapping that ends after a key without a value") {
            shouldRefuse(EventTree.UNPAIRED_KEY, documentStart, mappingStart, anchored, mappingEnd)
        }

        test("refuses an alias to an anchor that its document does not define before it") {
            shouldRefuse(EventTree.UNDEFINED_ALIAS, documentStart, alias)
            shouldRefuse(EventTree.UNDEFINED_ALIAS, documentStart, anchored, documentEnd, documentStart, alias)
        }

        test("refuses events that end inside a document") {
            shouldRefuse(EventTree.UNFINISHED, documentStart)
            shouldRefuse(EventTree.UNFINISHED, streamStart, documentStart, sequenceStart)
        }

        test("refuses a scalar with a lone surrogate") {
            val events = listOf(documentStart, scalarEvent("a\uD800", anchor = null), documentEnd)

            shouldThrow<IllegalArgumentException> {
                Yaml.Default.emitEvents(events)
            }.message shouldBe EmitNode.Scalar.LONE_SURROGATE
        }

        test("writes a quoted scalar plain only where it reads as a string") {
            val legacy = yaml {
                emitter {
                    compatibility = YamlVersion.V1_1
                    isStylePreserved = false
                }
            }
            val current = yaml { emitter { isStylePreserved = false } }
            val quoted = YamlEvent.Scalar(
                range = range,
                anchor = null,
                tag = null,
                value = "yes",
                style = YamlScalarStyle.SingleQuoted,
            )
            val tagged = YamlEvent.Scalar(
                range = range,
                anchor = null,
                tag = YamlTag("!t"),
                value = "",
                style = YamlScalarStyle.Plain,
            )
            val events = listOf(documentStart, sequenceStart, quoted, tagged, sequenceEnd, documentEnd)

            legacy.emitEvents(events) shouldBe text("- \"yes\"", "- !t", "")
            current.emitEvents(events) shouldBe text("- yes", "- !t", "")
        }
    }
})

private fun bounded(length: Int): Yaml = yaml { limits { maximumOutputLength = length } }

private fun shouldRefuse(message: String, vararg events: YamlEvent) {
    shouldThrow<IllegalArgumentException> { Yaml.Default.emitEvents(events.toList()) }.message shouldBe message
}

private fun scalarEvent(value: String, anchor: YamlAnchor?): YamlEvent =
    YamlEvent.Scalar(range = range, anchor = anchor, tag = null, value = value, style = YamlScalarStyle.Plain)

private fun nestedSequences(depth: Int, style: YamlCollectionStyle): YamlNode {
    var node: YamlNode = yamlSequence(emptyList(), style = style)
    repeat(depth - 1) { node = yamlSequence(listOf(node), style = style) }
    return node
}

private fun nestedMappings(depth: Int, style: YamlCollectionStyle): YamlNode {
    var node: YamlNode = yamlMapping(emptyList(), style = style)
    repeat(depth - 1) { node = yamlMapping(listOf(YamlEntry(key = yamlScalar("k"), value = node)), style = style) }
    return node
}

private fun keyedBy(key: YamlNode): YamlNode =
    yamlMapping(entries = listOf(YamlEntry(key = key, value = yamlScalar("v"))))

private fun nestedEvents(depth: Int): List<YamlEvent> = buildList {
    add(documentStart)
    repeat(depth) { add(sequenceStart) }
    repeat(depth) { add(sequenceEnd) }
    add(documentEnd)
}
