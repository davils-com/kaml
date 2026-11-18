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
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.limit.YamlLimits

internal class EventBudget(private val limits: YamlLimits) {
    private var nodes = 0L
    private var documents = 0
    private var depth = 1

    var exhaustion: YamlDiagnostic? = null
        private set

    val isExhausted: Boolean get() = exhaustion != null

    fun admits(event: YamlEvent): Boolean {
        if (isExhausted) return false
        when (event) {
            is YamlEvent.DocumentStart -> document(event)
            is YamlEvent.SequenceStart, is YamlEvent.MappingStart -> collection(event)
            is YamlEvent.SequenceEnd, is YamlEvent.MappingEnd -> depth--
            is YamlEvent.Scalar -> scalar(event)
            is YamlEvent.Alias -> node(event)
            is YamlEvent.StreamStart, is YamlEvent.StreamEnd, is YamlEvent.DocumentEnd -> Unit
        }
        return !isExhausted
    }

    private fun document(event: YamlEvent.DocumentStart) {
        documents++
        if (documents <= limits.maximumDocuments) return
        val message = "The stream has more than ${limits.maximumDocuments} documents"
        exhaust(YamlDiagnosticCode.TooManyDocuments, event, message)
    }

    private fun collection(event: YamlEvent) {
        node(event)
        if (depth >= limits.maximumDepth) {
            exhaust(YamlDiagnosticCode.TooDeep, event, "The document nests deeper than ${limits.maximumDepth} levels")
        }
        depth++
    }

    private fun scalar(event: YamlEvent.Scalar) {
        node(event)
        if (event.value.length <= limits.maximumScalarLength) return
        exhaust(
            YamlDiagnosticCode.ScalarTooLong,
            event,
            "A scalar is longer than ${limits.maximumScalarLength} characters",
        )
    }

    private fun node(event: YamlEvent) {
        nodes++
        if (nodes <= limits.maximumNodes) return
        exhaust(YamlDiagnosticCode.TooManyNodes, event, "The stream has more than ${limits.maximumNodes} nodes")
    }

    private fun exhaust(code: YamlDiagnosticCode, event: YamlEvent, message: String) {
        if (isExhausted) return
        exhaustion = YamlDiagnostic(code = code, range = event.range, message = message)
    }
}
