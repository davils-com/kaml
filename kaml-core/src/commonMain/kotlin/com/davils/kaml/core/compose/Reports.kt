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
import com.davils.kore.core.text.TextRange

internal class Reports(private val most: Int) {
    private val kept = ArrayList<YamlDiagnostic>()

    val all: List<YamlDiagnostic> get() = kept.toList()

    fun add(code: YamlDiagnosticCode, range: TextRange?, message: String) {
        if (kept.size > most) return
        kept.add(YamlDiagnostic(code = code, range = range ?: TextRange.at(0), message = message))
    }
}
