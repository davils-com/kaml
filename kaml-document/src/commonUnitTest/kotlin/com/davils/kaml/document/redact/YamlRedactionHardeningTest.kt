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

package com.davils.kaml.document.redact

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val FEW_MAPPINGS: Int = 5_000
private const val MANY_MAPPINGS: Int = 40_000
private const val GROWTH: Int = 16
private val slack: Duration = 300.milliseconds

private fun equalMappings(count: Int): YamlNode =
    yamlSequence(List(count) { yamlMapping(listOf(YamlEntry(key = yamlScalar("a"), value = yamlScalar("b")))) })

private fun timed(work: () -> Unit): Duration {
    val mark = TimeSource.Monotonic.markNow()
    work()
    return mark.elapsedNow()
}

class YamlRedactionHardeningTest : FunSpec({
    context("graphs built in code") {
        test("are redacted in near-linear time, however many equal nodes without a range they hold") {
            val redaction = yamlRedaction { key { it == "password" } }
            val few = equalMappings(FEW_MAPPINGS)
            val many = equalMappings(MANY_MAPPINGS)
            val fewTime = timed { redaction.redact(few) }
            val manyTime = timed { redaction.redact(many) }

            (manyTime < fewTime * GROWTH + slack) shouldBe true
            redaction.redact(many) shouldBe many
        }
    }
})
