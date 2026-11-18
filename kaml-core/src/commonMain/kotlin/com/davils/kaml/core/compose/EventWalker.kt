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

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextRange

internal class EventWalker(text: String, maximumDiagnostics: Int, private val sink: (YamlEvent) -> Boolean) {
    private val lines = LineMap.of(text)
    private val length = text.length
    private val reports = Reports(maximumDiagnostics)
    private val pending = ArrayList<Step>()
    private var properties = NodePropertyReader(DocumentDirectives(reports), reports)
    private var isStopped = false

    private val handlers: Map<YamlSyntaxKind, (Located, NodeProperties, Int) -> Unit> = mapOf(
        YamlSyntaxKind.SCALAR to { node, found, _ -> scalar(node, found) },
        YamlSyntaxKind.BLOCK_SCALAR to ::blockScalar,
        YamlSyntaxKind.ALIAS_NODE to { node, found, _ -> alias(node, found) },
        YamlSyntaxKind.EMPTY_NODE to { node, found, _ -> emptyScalar(node.offset, found) },
        YamlSyntaxKind.BLOCK_SEQUENCE to { node, found, _ -> sequence(node, found) },
        YamlSyntaxKind.FLOW_SEQUENCE to { node, found, _ -> sequence(node, found) },
        YamlSyntaxKind.BLOCK_MAPPING to { node, found, _ -> mapping(node, found) },
        YamlSyntaxKind.FLOW_MAPPING to { node, found, _ -> mapping(node, found) },
    )

    val diagnostics: List<YamlDiagnostic> get() = reports.all

    fun stream(root: Located) {
        emit(YamlEvent.StreamStart(TextRange.at(0)))
        val documents = root.all(YamlSyntaxKind.DOCUMENT).iterator()
        while (!isStopped && documents.hasNext()) {
            document(documents.next())
        }
        emit(YamlEvent.StreamEnd(TextRange.at(length)))
    }

    private fun document(node: Located) {
        val directives = DocumentDirectives(reports)
        node.all(YamlSyntaxKind.DIRECTIVE).forEach { directive -> read(directive, directives) }
        properties = NodePropertyReader(directives, reports)
        emit(
            YamlEvent.DocumentStart(
                range = node.range,
                isExplicit = node.has(YamlSyntaxKind.DOCUMENT_START),
                version = directives.version,
                tagDirectives = directives.tagDirectives,
            ),
        )
        walk(Step.Visit(node = node.valueNode(), parentIndent = ROOT_INDENT, fallback = node.end))
        emit(YamlEvent.DocumentEnd(TextRange.at(node.end), node.has(YamlSyntaxKind.DOCUMENT_END)))
    }

    private fun read(directive: Located, directives: DocumentDirectives) {
        val name = directive.token(YamlSyntaxKind.DIRECTIVE_NAME).text
        val parameters = directive.all(YamlSyntaxKind.DIRECTIVE_PARAMETER).map(Located::text)
        directives.read(name, parameters, directive.range)
    }

    private fun walk(first: Step) {
        pending.add(first)
        while (!isStopped && pending.isNotEmpty()) {
            take(pending.removeAt(pending.lastIndex))
        }
        pending.clear()
    }

    private fun take(step: Step) {
        when (step) {
            is Step.Visit -> visit(step)
            is Step.Emit -> emit(step.event)
        }
    }

    private fun visit(step: Step.Visit) {
        val node = step.node
        if (node == null) {
            emptyScalar(step.fallback, NodeProperties.NONE)
            return
        }
        handlers.getValue(node.kind).invoke(node, properties.of(node), step.parentIndent)
    }

    private fun emit(event: YamlEvent) {
        if (!isStopped) isStopped = !sink(event)
    }

    private fun schedule(steps: List<Step>, end: YamlEvent) {
        pending.add(Step.Emit(end))
        pending.addAll(steps.asReversed())
    }

    private fun scalar(node: Located, found: NodeProperties) {
        val token = node.children.first { it.kind in scalarStyles }
        val style = scalarStyles.getValue(token.kind)
        val value = valueReaders.getValue(style).invoke(token.text)
        emit(scalarEvent(range = node.range, found = found, value = value, style = style))
    }

    private fun blockScalar(node: Located, found: NodeProperties, parentIndent: Int) {
        val header = node.token(YamlSyntaxKind.BLOCK_SCALAR_HEADER).text
        val content = node.child(YamlSyntaxKind.BLOCK_SCALAR_CONTENT)?.text.orEmpty()
        val style = YamlScalarStyle.Literal.takeIf { header.first() == '|' } ?: YamlScalarStyle.Folded
        val value = BlockScalarValues.value(header = header, content = content, parentIndent = parentIndent)
        emit(scalarEvent(range = node.range, found = found, value = value, style = style))
    }

    private fun alias(node: Located, found: NodeProperties) {
        val name = node.token(YamlSyntaxKind.ALIAS).text.drop(1)
        if (!YamlAnchor.isValidName(name)) {
            emptyScalar(node.offset, found)
            return
        }
        emit(YamlEvent.Alias(node.range, YamlAnchor(name)))
    }

    private fun emptyScalar(offset: Int, found: NodeProperties) {
        emit(scalarEvent(range = TextRange.at(offset), found = found, value = "", style = YamlScalarStyle.Plain))
    }

    private fun scalarEvent(
        range: TextRange,
        found: NodeProperties,
        value: String,
        style: YamlScalarStyle,
    ): YamlEvent.Scalar =
        YamlEvent.Scalar(range = range, anchor = found.anchor, tag = found.tag, value = value, style = style)

    private fun sequence(node: Located, found: NodeProperties) {
        val style = styleOf(node, YamlSyntaxKind.FLOW_SEQUENCE)
        emit(YamlEvent.SequenceStart(range = node.range, anchor = found.anchor, tag = found.tag, style = style))
        schedule(node.children.flatMap(::sequenceItem), YamlEvent.SequenceEnd(TextRange.at(node.end)))
    }

    private fun sequenceItem(child: Located): List<Step> {
        val column = columnOf(child.offset)
        val steps = ArrayList<Step>()
        if (child.kind == YamlSyntaxKind.BLOCK_SEQUENCE_ENTRY) {
            steps.add(Step.Visit(node = child.valueNode(), parentIndent = column, fallback = child.end))
        }
        if (child.kind == YamlSyntaxKind.FLOW_PAIR) steps.addAll(pair(child))
        if (child.kind.isValueNode) steps.add(Step.Visit(node = child, parentIndent = column, fallback = child.end))
        return steps
    }

    private fun pair(node: Located): List<Step> {
        val style = YamlCollectionStyle.Flow
        val start = YamlEvent.MappingStart(range = node.range, anchor = null, tag = null, style = style)
        return listOf(Step.Emit(start)) + entry(node) + Step.Emit(YamlEvent.MappingEnd(TextRange.at(node.end)))
    }

    private fun mapping(node: Located, found: NodeProperties) {
        val style = styleOf(node, YamlSyntaxKind.FLOW_MAPPING)
        emit(YamlEvent.MappingStart(range = node.range, anchor = found.anchor, tag = found.tag, style = style))
        val entries = node.children.filter { it.kind in entryKinds }
        schedule(entries.flatMap(::entry), YamlEvent.MappingEnd(TextRange.at(node.end)))
    }

    private fun entry(node: Located): List<Step> {
        val indent = columnOf(node.offset)
        val key = node.child(YamlSyntaxKind.KEY)
        val value = node.child(YamlSyntaxKind.VALUE)
        return listOf(
            Step.Visit(node = key?.valueNode(), parentIndent = indent, fallback = key?.offset ?: node.offset),
            Step.Visit(node = value?.valueNode(), parentIndent = indent, fallback = value?.offset ?: node.end),
        )
    }

    private fun styleOf(node: Located, flowKind: YamlSyntaxKind): YamlCollectionStyle =
        YamlCollectionStyle.Flow.takeIf { node.kind == flowKind } ?: YamlCollectionStyle.Block

    private fun columnOf(offset: Int): Int = lines.positionOf(offset).column

    private sealed interface Step {
        data class Visit(val node: Located?, val parentIndent: Int, val fallback: Int) : Step

        data class Emit(val event: YamlEvent) : Step
    }

    private companion object {
        const val ROOT_INDENT: Int = -1

        val scalarStyles: Map<YamlSyntaxKind, YamlScalarStyle> = mapOf(
            YamlSyntaxKind.PLAIN_SCALAR to YamlScalarStyle.Plain,
            YamlSyntaxKind.SINGLE_QUOTED_SCALAR to YamlScalarStyle.SingleQuoted,
            YamlSyntaxKind.DOUBLE_QUOTED_SCALAR to YamlScalarStyle.DoubleQuoted,
        )

        val valueReaders: Map<YamlScalarStyle, (String) -> String> = mapOf(
            YamlScalarStyle.Plain to FlowScalarValues::plain,
            YamlScalarStyle.SingleQuoted to FlowScalarValues::singleQuoted,
            YamlScalarStyle.DoubleQuoted to FlowScalarValues::doubleQuoted,
        )

        val entryKinds: Set<YamlSyntaxKind> = setOf(
            YamlSyntaxKind.BLOCK_MAPPING_ENTRY,
            YamlSyntaxKind.FLOW_MAPPING_ENTRY,
        )
    }
}
