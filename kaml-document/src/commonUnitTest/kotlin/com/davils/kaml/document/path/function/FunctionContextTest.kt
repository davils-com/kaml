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

package com.davils.kaml.document.path.function

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.document.internal.WorkBudget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val BUDGET: Long = 1_000L
private const val STEPS: String = "maximumQuerySteps"

class FunctionContextTest : FunSpec({
    context("patterns") {
        test("are compiled once per evaluation and charged once") {
            val budget = WorkBudget(BUDGET, STEPS)
            val context = FunctionContext(YamlLimits.Untrusted, budget)
            val first = context.regexpOf("a+")
            val spent = budget.spent

            context.regexpOf("a+").shouldBeSameInstanceAs(first)
            budget.spent shouldBe spent
        }

        test("that are invalid are refused once and remembered") {
            val budget = WorkBudget(BUDGET, STEPS)
            val context = FunctionContext(YamlLimits.Untrusted, budget)

            context.regexpOf("(").shouldBeNull()
            val spent = budget.spent
            context.regexpOf("(").shouldBeNull()
            budget.spent shouldBe spent
        }

        test("belong to one evaluation only") {
            val budget = WorkBudget(BUDGET, STEPS)
            val first = FunctionContext(YamlLimits.Untrusted, budget).regexpOf("a")
            val second = FunctionContext(YamlLimits.Untrusted, budget).regexpOf("a")

            (first === second) shouldBe false
        }
    }
})
