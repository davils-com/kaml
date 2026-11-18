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
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.yaml
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private val here: TextRange = TextRange.at(0)
private val sequence: YamlEvent = YamlEvent.SequenceStart(
    range = here,
    anchor = null,
    tag = null,
    style = YamlCollectionStyle.Flow,
)
private val scalar: YamlEvent = YamlEvent.Scalar(
    range = here,
    anchor = null,
    tag = null,
    value = "a",
    style = YamlScalarStyle.Plain,
)

class EventBudgetTest : FunSpec({
    context("the event budget") {
        test("admits events up to its limits") {
            val budget = EventBudget(yamlLimits { maximumNodes = 2 })

            budget.admits(YamlEvent.StreamStart(here)) shouldBe true
            budget.admits(sequence) shouldBe true
            budget.admits(scalar) shouldBe true
            budget.admits(YamlEvent.SequenceEnd(here)) shouldBe true
            budget.exhaustion.shouldBeNull()
        }

        test("keeps the first excess and refuses every later event") {
            val budget = EventBudget(
                yamlLimits {
                    maximumNodes = 2
                    maximumDepth = 2
                },
            )

            budget.admits(sequence) shouldBe true
            budget.admits(scalar) shouldBe true
            budget.admits(sequence) shouldBe false
            budget.exhaustion?.code shouldBe YamlDiagnosticCode.TooManyNodes
            budget.admits(YamlEvent.SequenceEnd(here)) shouldBe false
        }

        test("counts an alias as a node") {
            val budget = EventBudget(yamlLimits { maximumNodes = 1 })

            budget.admits(scalar) shouldBe true
            budget.admits(YamlEvent.Alias(here, YamlAnchor("a"))) shouldBe false
        }
    }

    context("the composition budget") {
        test("reports only the first excess of an event") {
            val tight = yaml {
                limits {
                    maximumAliasExpansion = 1
                    maximumCollectionSize = 2
                }
            }
            val problems = tight.problems("[&a [x], b, *a]")

            problems shouldContain YamlDiagnosticCode.TooManyAliasExpansions
            problems shouldNotContain YamlDiagnosticCode.CollectionTooLarge
        }
    }
})
