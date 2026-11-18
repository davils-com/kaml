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

import com.davils.kaml.core.YamlConfiguration
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.event.YamlEventResult
import com.davils.kaml.core.node.YamlStream
import com.davils.kaml.core.parser.SyntaxParser
import com.davils.kaml.core.source.SourceDecoding
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.syntax.internal.GreenNode
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity

internal class YamlPipeline(configuration: YamlConfiguration) {
    private val limits = configuration.limits
    private val settings = ComposeSettings(
        schema = configuration.schema,
        isVersionSelectingSchema = configuration.isSchemaSelectedByVersion,
        duplicateKeys = configuration.duplicateKeys,
        unknownTags = configuration.unknownTags,
        limits = limits,
    )

    fun events(source: YamlSource): YamlEventResult {
        val reading = read(source)
        return YamlEventResult(reading.events, reading.diagnostics)
    }

    fun parse(source: YamlSource): YamlParseResult {
        val reading = read(source)
        if (!reading.isComplete) return YamlParseResult.Failed(reading.diagnostics, YamlStream(emptyList()))
        return compose(reading.events, reading.diagnostics)
    }

    fun compose(events: List<YamlEvent>, earlier: List<YamlDiagnostic>): YamlParseResult {
        val composer = GraphComposer(settings)
        val stream = YamlStream(composer.compose(events))
        val diagnostics = capped(earlier + composer.diagnostics)
        val hasErrors = diagnostics.any { it.severity == Severity.Error }
        if (hasErrors) return YamlParseResult.Failed(diagnostics, stream)
        return YamlParseResult.Parsed(stream, diagnostics)
    }

    fun read(source: YamlSource): Reading {
        val leastCharacters = source.byteLength / MOST_BYTES_PER_CHARACTER
        if (leastCharacters > limits.maximumInputLength) return tooLong()
        val decoded = SourceDecoding.decode(source, limits.maximumInputLength)
        return readText(decoded.text, decoded.diagnostics)
    }

    fun readText(text: String, earlier: List<YamlDiagnostic> = emptyList()): Reading {
        if (text.length > limits.maximumInputLength) return tooLong()
        val syntax = SyntaxParser.parse(
            text = text,
            maximumDepth = limits.maximumDepth,
            maximumDiagnostics = limits.maximumDiagnostics,
            maximumNodes = limits.maximumNodes,
        )
        val walk = walk(text, syntax.root)
        val parserCodes = syntax.diagnostics.mapTo(HashSet(), YamlDiagnostic::code)
        val walkDiagnostics = walk.diagnostics.filterNot { it.code in limitCodes && it.code in parserCodes }
        val diagnostics = capped(earlier + syntax.diagnostics + walkDiagnostics)
        return Reading(
            root = syntax.root,
            events = walk.events,
            diagnostics = diagnostics,
            isComplete = walk.isComplete,
        )
    }

    fun composeTree(text: String, root: GreenNode, earlier: List<YamlDiagnostic>): YamlParseResult {
        if (text.length > limits.maximumInputLength) {
            return YamlParseResult.Failed(tooLong().diagnostics, YamlStream(emptyList()))
        }
        val walk = walk(text, root)
        val diagnostics = earlier.filterNot { it.code in walkerCodes } + walk.diagnostics
        val composed = walk.events.takeIf { walk.isComplete }?.let { events -> compose(events, diagnostics) }
        return composed ?: YamlParseResult.Failed(capped(diagnostics), YamlStream(emptyList()))
    }

    private fun walk(text: String, root: GreenNode): Walk {
        val events = ArrayList<YamlEvent>()
        val budget = EventBudget(limits)
        val walker = EventWalker(text = text, maximumDiagnostics = limits.maximumDiagnostics) { event ->
            val isAdmitted = budget.admits(event)
            if (isAdmitted) events.add(event)
            isAdmitted
        }
        walker.stream(Located(root, 0))
        val diagnostics = walker.diagnostics + listOfNotNull(budget.exhaustion)
        return Walk(events = events, diagnostics = diagnostics, isComplete = !budget.isExhausted)
    }

    private fun capped(diagnostics: List<YamlDiagnostic>): List<YamlDiagnostic> {
        val sorted = diagnostics.sortedBy { it.range }
        val most = limits.maximumDiagnostics
        if (sorted.size <= most) return sorted
        val omitted = YamlDiagnostic(
            code = YamlDiagnosticCode.TooManyDiagnostics,
            range = sorted[most].range,
            message = "The source has more than $most problems; the rest are not reported",
        )
        return sorted.take(most) + omitted
    }

    private fun tooLong(): Reading {
        val diagnostic = YamlDiagnostic(
            code = YamlDiagnosticCode.InputTooLong,
            range = TextRange.at(0),
            message = "The source is longer than ${limits.maximumInputLength} characters",
        )
        val empty = SyntaxParser.parse("").root
        return Reading(root = empty, events = emptyList(), diagnostics = listOf(diagnostic), isComplete = false)
    }

    private data class Walk(val events: List<YamlEvent>, val diagnostics: List<YamlDiagnostic>, val isComplete: Boolean)

    data class Reading(
        val root: GreenNode,
        val events: List<YamlEvent>,
        val diagnostics: List<YamlDiagnostic>,
        val isComplete: Boolean,
    )

    private companion object {
        const val MOST_BYTES_PER_CHARACTER: Int = 4

        val limitCodes: Set<YamlDiagnosticCode> = setOf(YamlDiagnosticCode.TooDeep, YamlDiagnosticCode.TooManyNodes)

        val walkerCodes: Set<YamlDiagnosticCode> = setOf(
            YamlDiagnosticCode.InvalidDirective,
            YamlDiagnosticCode.DuplicateDirective,
            YamlDiagnosticCode.ReservedDirective,
            YamlDiagnosticCode.UnsupportedVersion,
            YamlDiagnosticCode.NewerMinorVersion,
            YamlDiagnosticCode.UnknownTagHandle,
            YamlDiagnosticCode.DuplicateProperty,
        )
    }
}
