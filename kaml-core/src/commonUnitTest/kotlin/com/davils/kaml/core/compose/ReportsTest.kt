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
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private const val MOST: Int = 2
private const val MESSAGE: String = "message"

class ReportsTest : FunSpec({
    context("reports") {
        test("keep one diagnostic more than the limit to show that there were more") {
            val reports = Reports(MOST)

            repeat(MOST + 2) { reports.add(YamlDiagnosticCode.NotJson, TextRange.at(it), MESSAGE) }

            reports.all shouldHaveSize MOST + 1
        }

        test("place a diagnostic without a range at the start") {
            val reports = Reports(MOST)

            reports.add(YamlDiagnosticCode.InvalidMerge, null, MESSAGE)

            reports.all.single().range shouldBe TextRange.at(0)
        }
    }
})
