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

import com.davils.kaml.core.YamlConfiguration
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.internal.NameMap
import com.davils.kaml.core.internal.NameSet
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag

internal class EventTree(private val configuration: YamlConfiguration) {
    private val limits = configuration.limits
    private val documents = ArrayList<EmitDocument>()
    private val stack = ArrayDeque<Open>()
    private val defined = NameMap<String>()
    private var anchors = AnchorNames()
    private var start: YamlEvent.DocumentStart? = null
    private var root: EmitNode? = null
    private var isStreamEnded = false

    fun build(events: List<YamlEvent>): List<EmitDocument> {
        val reserved = events.mapNotNull { event -> anchorOf(event)?.name }
        anchors = AnchorNames(NameSet(reserved.filter(AnchorNames::isValid)))
        events.forEachIndexed { index, event -> accept(event, isFirst = index == 0) }
        require(start == null) { UNFINISHED }
        return documents.toList()
    }

    private fun accept(event: YamlEvent, isFirst: Boolean) {
        when (event) {
            is YamlEvent.StreamStart -> require(isFirst) { MISPLACED_STREAM_START }

            is YamlEvent.StreamEnd -> endStream()

            is YamlEvent.DocumentStart -> startDocument(event)

            is YamlEvent.DocumentEnd -> endDocument(event)

            is YamlEvent.SequenceStart -> open(
                anchor = event.anchor,
                tag = event.tag,
                style = event.style,
                isMapping = false,
            )

            is YamlEvent.MappingStart -> open(
                anchor = event.anchor,
                tag = event.tag,
                style = event.style,
                isMapping = true,
            )

            is YamlEvent.SequenceEnd -> close(isMapping = false)

            is YamlEvent.MappingEnd -> close(isMapping = true)

            is YamlEvent.Scalar -> attach(scalar(event))

            is YamlEvent.Alias -> attach(EmitNode.Alias(aliasName(event.anchor)))
        }
    }

    private fun endStream() {
        require(start == null && !isStreamEnded) { MISPLACED_STREAM_END }
        isStreamEnded = true
    }

    private fun startDocument(event: YamlEvent.DocumentStart) {
        require(start == null && !isStreamEnded) { MISPLACED_DOCUMENT_START }
        start = event
        root = null
        defined.clear()
        anchors.clear()
    }

    private fun endDocument(event: YamlEvent.DocumentEnd) {
        val open = start
        val complete = root
        require(open != null && complete != null && stack.isEmpty()) { MISPLACED_DOCUMENT_END }
        documents += EmitDocument(
            root = complete,
            version = open.version,
            tagDirectives = open.tagDirectives,
            isExplicitStart = open.isExplicit,
            isExplicitEnd = event.isExplicit,
            resolution = PlainResolution.of(configuration, open.version),
        )
        start = null
    }

    private fun open(anchor: YamlAnchor?, tag: YamlTag?, style: YamlCollectionStyle, isMapping: Boolean) {
        requireNodeAllowed()
        require(stack.size + 1 < limits.maximumDepth) { tooDeep(limits.maximumDepth) }
        stack.addLast(Open(anchor = anchorName(anchor), tag = tag, style = style, isMapping = isMapping))
    }

    private fun close(isMapping: Boolean) {
        val open = stack.lastOrNull()
        require(open != null && open.isMapping == isMapping) { UNMATCHED_END }
        stack.removeLast()
        attach(open.toNode())
    }

    private fun scalar(event: YamlEvent.Scalar): EmitNode.Scalar {
        val isStringOnly = event.tag == null && event.style != YamlScalarStyle.Plain
        return EmitNode.Scalar(
            value = event.value,
            style = event.style,
            anchor = anchorName(event.anchor),
            tag = event.tag,
            plainTag = YamlTag.STRING.takeIf { isStringOnly },
        )
    }

    private fun attach(node: EmitNode) {
        requireNodeAllowed()
        val parent = stack.lastOrNull()
        if (parent == null) {
            root = node
            return
        }
        parent.children.add(node)
    }

    private fun requireNodeAllowed() {
        val isInDocument = start != null
        require(isInDocument && (stack.isNotEmpty() || root == null)) { MISPLACED_NODE }
    }

    private fun anchorName(anchor: YamlAnchor?): String? {
        val original = anchor?.name ?: return null
        val name = anchors.preferred(original)
        defined[original] = name
        return name
    }

    private fun aliasName(anchor: YamlAnchor): String = requireNotNull(defined[anchor.name]) { UNDEFINED_ALIAS }

    private fun anchorOf(event: YamlEvent): YamlAnchor? = when (event) {
        is YamlEvent.SequenceStart -> event.anchor

        is YamlEvent.MappingStart -> event.anchor

        is YamlEvent.Scalar -> event.anchor

        is YamlEvent.Alias -> event.anchor

        is YamlEvent.StreamStart, is YamlEvent.StreamEnd, is YamlEvent.DocumentStart, is YamlEvent.DocumentEnd,
        is YamlEvent.SequenceEnd, is YamlEvent.MappingEnd,
        -> null
    }

    private class Open(
        val anchor: String?,
        val tag: YamlTag?,
        val style: YamlCollectionStyle,
        val isMapping: Boolean,
    ) {
        val children: MutableList<EmitNode> = ArrayList()

        fun toNode(): EmitNode {
            if (!isMapping) {
                return EmitNode.Sequence(items = children.toList(), style = style, anchor = anchor, tag = tag)
            }
            require(children.size % 2 == 0) { UNPAIRED_KEY }
            val pairs = children.chunked(2) { (key, value) -> key to value }
            return EmitNode.Mapping(entries = pairs, style = style, anchor = anchor, tag = tag)
        }
    }

    companion object {
        const val MISPLACED_STREAM_START: String = "A stream start event must be the first event"
        const val MISPLACED_STREAM_END: String = "A stream end event must follow the last document end"
        const val MISPLACED_DOCUMENT_START: String = "A document start event must follow the end of the last document"
        const val MISPLACED_DOCUMENT_END: String =
            "A document end event must follow exactly one complete root node of its document"
        const val MISPLACED_NODE: String = "A node event must be the root of a document or lie in an open collection"
        const val UNMATCHED_END: String = "A collection end event does not match the open collection"
        const val UNPAIRED_KEY: String = "A mapping end event follows a key without a value"
        const val UNDEFINED_ALIAS: String = "An alias event refers to an anchor no earlier node of its document defines"
        const val UNFINISHED: String = "The events end inside a document"

        fun tooDeep(maximumDepth: Int): String = "The events nest collections deeper than $maximumDepth levels"
    }
}
