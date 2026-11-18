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
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kore.core.text.TextRange

internal class GraphComposer(private val settings: ComposeSettings) {
    private val reports = Reports(settings.limits.maximumDiagnostics)
    private val documents = ArrayList<YamlDocument>()
    private val frames = ArrayDeque<ComposeFrame>()
    private val budget = ComposeBudget(settings.limits, reports::add)
    private val anchors = AnchorTable(budget, reports::add)
    private var scalars = ScalarComposer(settings, settings.schema, reports::add)
    private var collections = collectionsFor(settings.schema)

    val diagnostics: List<YamlDiagnostic> get() = reports.all

    fun compose(events: Iterable<YamlEvent>): List<YamlDocument> {
        val iterator = events.iterator()
        while (!budget.isExhausted && iterator.hasNext()) {
            accept(iterator.next())
        }
        return documents.toList()
    }

    private fun accept(event: YamlEvent) {
        when (event) {
            is YamlEvent.StreamStart, is YamlEvent.StreamEnd -> Unit
            is YamlEvent.DocumentStart -> startDocument(event)
            is YamlEvent.DocumentEnd -> endDocument(event)
            is YamlEvent.SequenceStart -> open(ComposeFrame.Sequence(event), event.anchor?.name, event.range)
            is YamlEvent.MappingStart -> open(ComposeFrame.Mapping(event), event.anchor?.name, event.range)
            is YamlEvent.SequenceEnd -> closeSequence()
            is YamlEvent.MappingEnd -> closeMapping()
            is YamlEvent.Scalar -> complete(scalars.scalar(event), 1L, anchors.next())
            is YamlEvent.Alias -> alias(event)
        }
    }

    private fun startDocument(event: YamlEvent.DocumentStart) {
        anchors.reset()
        val schema = schemaFor(event.version)
        scalars = ScalarComposer(settings, schema, reports::add)
        collections = collectionsFor(schema)
        frames.addLast(ComposeFrame.Document(event))
    }

    private fun collectionsFor(schema: YamlSchema): CollectionComposer =
        CollectionComposer(settings = settings, schema = schema, budget = budget, report = reports::add)

    private fun schemaFor(version: YamlVersion?): YamlSchema {
        val isLegacy = settings.isVersionSelectingSchema && version == YamlVersion.V1_1
        return YamlSchema.Yaml11.takeIf { isLegacy } ?: settings.schema
    }

    private fun endDocument(event: YamlEvent.DocumentEnd) {
        val frame = frames.removeLast() as ComposeFrame.Document
        val root = frame.root ?: scalars.empty(event.range)
        documents += YamlDocument(
            root = root,
            version = frame.start.version,
            tagDirectives = frame.start.tagDirectives,
            isExplicitStart = frame.start.isExplicit,
            isExplicitEnd = event.isExplicit,
            range = frame.start.range,
        )
    }

    private fun open(frame: ComposeFrame, anchor: String?, range: TextRange) {
        if (!budget.depth(frames.size, range)) return
        anchor?.let(anchors::open)
        frames.addLast(frame)
    }

    private fun closeSequence() {
        val frame = frames.removeLast() as ComposeFrame.Sequence
        val order = anchors.close(frame.start.anchor?.name)
        complete(collections.sequence(frame), frame.size, order)
    }

    private fun closeMapping() {
        val frame = frames.removeLast() as ComposeFrame.Mapping
        val order = anchors.close(frame.start.anchor?.name)
        complete(collections.mapping(frame), frame.size, order)
    }

    private fun alias(event: YamlEvent.Alias) {
        val target = anchors.resolve(event)
        val isAdmitted = target != null && admits(target, event.range)
        if (target == null || !isAdmitted) {
            attach(scalars.empty(event.range), 1L)
            return
        }
        attach(target.node, target.size)
    }

    private fun admits(target: Anchored, range: TextRange): Boolean {
        budget.expand(target.size, range)
        return budget.depth(frames.size - 1 + target.node.depth, range)
    }

    private fun complete(node: YamlNode, size: Long, order: Int) {
        node.anchor?.let { anchor -> anchors.declare(anchor.name, Anchored(node, size, order)) }
        attach(node, size)
    }

    private fun attach(node: YamlNode, size: Long) {
        val parent = frames.last()
        parent.size += size
        parent.attach(node)
        if (parent is ComposeFrame.Mapping) budget.collection(parent.count, node.range)
        if (parent is ComposeFrame.Sequence) budget.collection(parent.items.size, node.range)
    }
}
