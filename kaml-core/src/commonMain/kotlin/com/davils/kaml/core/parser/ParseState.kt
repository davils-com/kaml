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

package com.davils.kaml.core.parser

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kore.core.text.TextRange

internal class ParseState(
    source: String,
    private val maximumDepth: Int = YamlLimits.HARD_DEPTH,
    private val maximumDiagnostics: Int = YamlLimits.HARD_DIAGNOSTICS,
) {
    val text: String = source.replace(oldChar = END, newChar = NONCHARACTER)
    val cursor: Cursor = Cursor(text)
    val builder: GreenBuilder = GreenBuilder(source)
    private val reported = ArrayList<YamlDiagnostic>()
    private val descent = DeepRecursiveFunction<Nesting, Unit> { nesting -> nesting.body(this) }
    private var depth = 0
    private var isAborted = false

    val diagnostics: List<YamlDiagnostic> get() = reported.toList()

    val position: Int get() = cursor.position

    fun peek(ahead: Int = 0): Char = cursor.peek(ahead)

    fun charAt(index: Int): Char = cursor.charAt(index)

    fun emit(kind: YamlSyntaxKind, length: Int) {
        builder.token(kind = kind, start = cursor.position, end = cursor.position + length)
        cursor.advance(length)
    }

    fun emitTo(kind: YamlSyntaxKind, end: Int) {
        emit(kind, end - cursor.position)
    }

    fun report(code: YamlDiagnosticCode, range: TextRange, message: String) {
        if (reported.size > maximumDiagnostics) return
        reported.add(YamlDiagnostic(code = code, range = range, message = message))
    }

    fun span(start: Int, end: Int): TextRange = TextRange(start = start, end = maxOf(a = start, b = end))

    fun snapshot(): Snapshot = Snapshot(
        cursor = cursor.mark(),
        builder = builder.mark(),
        diagnostics = reported.size,
        depth = depth,
        isAborted = isAborted,
    )

    fun restore(snapshot: Snapshot) {
        cursor.restore(snapshot.cursor)
        builder.rewind(snapshot.builder)
        while (reported.size > snapshot.diagnostics) {
            reported.removeAt(reported.lastIndex)
        }
        depth = snapshot.depth
        isAborted = snapshot.isAborted
    }

    fun unexpected(end: Int, message: String) {
        if (end <= position) return
        report(YamlDiagnosticCode.UnexpectedContent, span(start = position, end = end), message)
        builder.startNode(YamlSyntaxKind.ERROR)
        emitTo(YamlSyntaxKind.UNEXPECTED, end)
        builder.finishNode()
    }

    fun descend(body: suspend Descent.() -> Unit) {
        descent(Nesting(body))
    }

    suspend fun Descent.nested(body: suspend Descent.() -> Unit) {
        if (isAborted) return
        if (depth >= maximumDepth) {
            abort()
            return
        }
        depth++
        callRecursive(Nesting(body))
        depth--
    }

    private fun abort() {
        isAborted = true
        val range = span(start = position, end = text.length)
        report(YamlDiagnosticCode.TooDeep, range, "The document nests deeper than $maximumDepth levels")
        builder.startNode(YamlSyntaxKind.ERROR)
        emitTo(YamlSyntaxKind.UNEXPECTED, text.length)
        builder.finishNode()
    }

    data class Nesting(val body: suspend Descent.() -> Unit)

    data class Snapshot(
        val cursor: CursorMark,
        val builder: BuilderMark,
        val diagnostics: Int,
        val depth: Int,
        val isAborted: Boolean,
    )
}
