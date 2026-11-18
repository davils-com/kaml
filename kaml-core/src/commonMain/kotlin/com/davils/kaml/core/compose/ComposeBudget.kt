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

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kore.core.text.TextRange

internal class ComposeBudget(
    private val limits: YamlLimits,
    private val report: (YamlDiagnosticCode, TextRange?, String) -> Unit,
) {
    private var expanded = 0L

    var isExhausted: Boolean = false
        private set

    val remainingExpansion: Long get() = limits.maximumAliasExpansion - expanded

    fun anchors(count: Int, range: TextRange?) {
        if (count <= limits.maximumAnchors) return
        exhaust(
            YamlDiagnosticCode.TooManyAnchors,
            range,
            "A document declares more than ${limits.maximumAnchors} anchors",
        )
    }

    fun expand(size: Long, range: TextRange?) {
        expanded += size
        if (expanded <= limits.maximumAliasExpansion) return
        exhaust(
            YamlDiagnosticCode.TooManyAliasExpansions,
            range,
            "Aliases and merge keys expand to more than ${limits.maximumAliasExpansion} nodes",
        )
    }

    fun depth(depth: Int, range: TextRange?): Boolean {
        if (depth < limits.maximumDepth) return true
        exhaust(YamlDiagnosticCode.TooDeep, range, "The document nests deeper than ${limits.maximumDepth} levels")
        return false
    }

    fun collection(count: Int, range: TextRange?) {
        if (count <= limits.maximumCollectionSize) return
        exhaust(
            YamlDiagnosticCode.CollectionTooLarge,
            range,
            "A collection has more than ${limits.maximumCollectionSize} entries",
        )
    }

    private fun exhaust(code: YamlDiagnosticCode, range: TextRange?, message: String) {
        if (isExhausted) return
        isExhausted = true
        report(code, range, message)
    }
}
