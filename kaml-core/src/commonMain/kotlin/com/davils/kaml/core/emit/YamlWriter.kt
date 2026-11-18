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

import com.davils.kaml.core.limit.YamlLimits

internal class YamlWriter(private val options: YamlEmitOptions, private val limits: YamlLimits) {
    private val lineBreak = options.lineBreak.text

    fun write(documents: List<EmitDocument>): String {
        val out = OutputBuffer(limits.maximumOutputLength)
        documents.forEachIndexed { index, document ->
            val tags = TagNotation(document.tagDirectives)
            directives(document, tags, out)
            val isMarkerNeeded =
                options.isDocumentStartExplicit || document.isExplicitStart || hasDirectives(document) || index > 0
            if (isMarkerNeeded) out.append(DOCUMENT_START)
            val writer = BlockWriter(options = options, out = out, tags = tags, resolution = document.resolution)
            body(document = document, writer = writer, isAfterMarker = isMarkerNeeded, out = out)
            val next = documents.getOrNull(index + 1)
            val isNextDirected = next != null && hasDirectives(next)
            if (document.isExplicitEnd || isNextDirected) out.append(DOCUMENT_END + lineBreak)
        }
        return out.toString()
    }

    private fun body(document: EmitDocument, writer: BlockWriter, isAfterMarker: Boolean, out: OutputBuffer) {
        val hasComments = document.comments.isNotEmpty()
        if (isAfterMarker && hasComments) out.append(lineBreak)
        writer.comments(document.comments, indent = 0)
        writer.root(document.root, isAfterMarker = isAfterMarker && !hasComments)
    }

    private fun directives(document: EmitDocument, tags: TagNotation, out: OutputBuffer) {
        document.version?.let { version -> out.append("%YAML $version$lineBreak") }
        document.tagDirectives.forEach { directive -> out.append(tags.directive(directive) + lineBreak) }
    }

    private fun hasDirectives(document: EmitDocument): Boolean =
        document.version != null || document.tagDirectives.isNotEmpty()

    private companion object {
        const val DOCUMENT_START: String = "---"
        const val DOCUMENT_END: String = "..."
    }
}
