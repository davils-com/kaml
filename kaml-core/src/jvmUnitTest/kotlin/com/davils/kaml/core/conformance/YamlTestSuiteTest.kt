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

package com.davils.kaml.core.conformance

import com.davils.kaml.core.compose.EventReading
import com.davils.kaml.core.conformance.fixture.EventNotation
import com.davils.kaml.core.conformance.fixture.SuiteCases
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

@OptIn(DavilsInternal::class)
class YamlTestSuiteTest : FunSpec({
    context("the yaml-test-suite") {
        SuiteCases.all.forEach { case ->
            test("${case.id} ${case.name}") {
                val reading = EventReading.of(case.yaml)
                val errors = reading.diagnostics.filter { it.severity == Severity.Error }

                reading.root.text shouldBe case.yaml
                if (case.isError) errors.shouldNotBeEmpty()
                if (!case.isError) {
                    errors.shouldBeEmpty()
                    EventNotation.of(reading.events) shouldBe case.events.orEmpty().trimEnd()
                }
            }
        }
    }
})
