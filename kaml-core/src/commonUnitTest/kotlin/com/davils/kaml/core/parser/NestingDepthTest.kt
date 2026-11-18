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

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val NESTING: Int = YamlLimits.HARD_DEPTH - 1

private val nestings: Map<String, (Int) -> String> = mapOf(
    "flow sequences" to { levels -> "[".repeat(levels) + "]".repeat(levels) },
    "flow mappings" to { levels -> "{a: ".repeat(levels) + "b" + "}".repeat(levels) },
    "block mappings" to { levels -> (0 until levels).joinToString("\n") { level -> " ".repeat(level) + "k:" } },
    "block sequences" to { levels -> "- ".repeat(levels) + "a" },
    "block sequences on their own lines" to { levels ->
        (0 until levels).joinToString("\n") { level -> " ".repeat(level) + "-" } + " a"
    },
)

class NestingDepthTest : FunSpec({
    val relaxed = yaml { limits = YamlLimits.Relaxed }

    nestings.forEach { (name, document) ->
        context(name) {
            test("parse when they nest to the limit") {
                relaxed.parse(document(NESTING)).shouldBeInstanceOf<YamlParseResult.Parsed>()
            }

            test("are reported when they nest past the limit") {
                val codes = relaxed.parse(document(NESTING + 1)).diagnostics.map { it.code }

                codes shouldContain YamlDiagnosticCode.TooDeep
            }

            test("give events when they nest to the limit") {
                val events = relaxed.events(YamlSource.of(document(NESTING)))

                events.diagnostics.map { it.code } shouldNotContain YamlDiagnosticCode.TooDeep
            }
        }
    }
})
