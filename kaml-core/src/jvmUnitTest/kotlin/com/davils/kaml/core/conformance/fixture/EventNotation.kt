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

package com.davils.kaml.core.conformance.fixture

import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag

internal object EventNotation {
    private val styles: Map<YamlScalarStyle, String> = mapOf(
        YamlScalarStyle.Plain to ":",
        YamlScalarStyle.SingleQuoted to "'",
        YamlScalarStyle.DoubleQuoted to "\"",
        YamlScalarStyle.Literal to "|",
        YamlScalarStyle.Folded to ">",
    )

    private val escapes: Map<Char, String> = mapOf(
        '\\' to "\\\\",
        '\b' to "\\b",
        '\n' to "\\n",
        '\r' to "\\r",
        '\t' to "\\t",
    )

    fun of(events: List<YamlEvent>): String = events.joinToString("\n", transform = ::line)

    private fun line(event: YamlEvent): String = when (event) {
        is YamlEvent.StreamStart -> "+STR"

        is YamlEvent.StreamEnd -> "-STR"

        is YamlEvent.DocumentStart -> listOfNotNull("+DOC", "---".takeIf { event.isExplicit }).joinToString(" ")

        is YamlEvent.DocumentEnd -> listOfNotNull("-DOC", "...".takeIf { event.isExplicit }).joinToString(" ")

        is YamlEvent.SequenceStart -> collection(
            start = "+SEQ",
            flow = "[]",
            properties = Properties(event.style, event.anchor, event.tag),
        )

        is YamlEvent.SequenceEnd -> "-SEQ"

        is YamlEvent.MappingStart -> collection(
            start = "+MAP",
            flow = "{}",
            properties = Properties(event.style, event.anchor, event.tag),
        )

        is YamlEvent.MappingEnd -> "-MAP"

        is YamlEvent.Alias -> "=ALI *${event.anchor.name}"

        is YamlEvent.Scalar -> scalar(event)
    }

    private fun collection(start: String, flow: String, properties: Properties): String {
        val flowMark = flow.takeIf { properties.style == YamlCollectionStyle.Flow }
        val anchor = properties.anchor?.let { "&${it.name}" }
        return listOfNotNull(start, flowMark, anchor, properties.tag?.toString()).joinToString(" ")
    }

    private data class Properties(
        val style: YamlCollectionStyle,
        val anchor: YamlAnchor?,
        val tag: YamlTag?,
    )

    private fun scalar(event: YamlEvent.Scalar): String {
        val value = styles.getValue(event.style) + event.value.map { escapes[it] ?: it.toString() }.joinToString("")
        return listOfNotNull(
            "=VAL",
            event.anchor?.let { "&${it.name}" },
            event.tag?.toString(),
            value,
        ).joinToString(" ")
    }
}
