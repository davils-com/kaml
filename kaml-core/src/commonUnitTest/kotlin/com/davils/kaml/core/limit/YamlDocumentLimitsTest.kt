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

package com.davils.kaml.core.limit

import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val DERIVED_DEPTH: Int = 10

class YamlDocumentLimitsTest : FunSpec({
    context("the limits of kaml-document") {
        test("have their defaults") {
            val limits = YamlLimits.Untrusted

            limits.maximumQueryLength shouldBe YamlLimitsBuilder.DEFAULT_QUERY_LENGTH
            limits.maximumQueryNesting shouldBe YamlLimitsBuilder.DEFAULT_QUERY_NESTING
            limits.maximumQuerySteps shouldBe YamlLimitsBuilder.DEFAULT_QUERY_STEPS
            limits.maximumQueryResults shouldBe YamlLimitsBuilder.DEFAULT_QUERY_RESULTS
            limits.maximumRegexpLength shouldBe YamlLimitsBuilder.DEFAULT_REGEXP_LENGTH
            limits.maximumRegexpSize shouldBe YamlLimitsBuilder.DEFAULT_REGEXP_SIZE
            limits.maximumPatchOperations shouldBe YamlLimitsBuilder.DEFAULT_PATCH_OPERATIONS
            limits.maximumPatternInputLength shouldBe YamlLimitsBuilder.DEFAULT_PATTERN_INPUT_LENGTH
        }

        test("accept their hard ceilings") {
            val ceilings = yamlLimits {
                maximumQueryLength = YamlLimits.HARD_QUERY_LENGTH
                maximumQueryNesting = YamlLimits.HARD_QUERY_NESTING
                maximumQuerySteps = YamlLimits.HARD_QUERY_STEPS
                maximumQueryResults = YamlLimits.HARD_QUERY_RESULTS
                maximumRegexpLength = YamlLimits.HARD_REGEXP_LENGTH
                maximumRegexpSize = YamlLimits.HARD_REGEXP_SIZE
                maximumPatchOperations = YamlLimits.HARD_PATCH_OPERATIONS
                maximumPatternInputLength = YamlLimits.HARD_PATTERN_INPUT_LENGTH
            }

            ceilings.maximumQuerySteps shouldBe YamlLimits.HARD_QUERY_STEPS
            ceilings shouldNotBe YamlLimits.Untrusted
        }

        test("refuse values past their hard ceilings") {
            shouldThrow<ValidationException> { yamlLimits { maximumQueryLength = YamlLimits.HARD_QUERY_LENGTH + 1 } }
            shouldThrow<ValidationException> { yamlLimits { maximumQueryNesting = YamlLimits.HARD_QUERY_NESTING + 1 } }
            shouldThrow<ValidationException> { yamlLimits { maximumQuerySteps = YamlLimits.HARD_QUERY_STEPS + 1 } }
            shouldThrow<ValidationException> { yamlLimits { maximumQueryResults = YamlLimits.HARD_QUERY_RESULTS + 1 } }
            shouldThrow<ValidationException> { yamlLimits { maximumRegexpLength = YamlLimits.HARD_REGEXP_LENGTH + 1 } }
            shouldThrow<ValidationException> { yamlLimits { maximumRegexpSize = YamlLimits.HARD_REGEXP_SIZE + 1 } }
            shouldThrow<ValidationException> {
                yamlLimits { maximumPatchOperations = YamlLimits.HARD_PATCH_OPERATIONS + 1 }
            }
            shouldThrow<ValidationException> {
                yamlLimits { maximumPatternInputLength = YamlLimits.HARD_PATTERN_INPUT_LENGTH + 1 }
            }
        }

        test("refuse values that are not positive") {
            shouldThrow<ValidationException> { yamlLimits { maximumQuerySteps = 0 } }
            shouldThrow<ValidationException> { yamlLimits { maximumRegexpSize = 0 } }
        }

        test("are relaxed within their ceilings and carried over from a base") {
            val relaxed = YamlLimits.Relaxed
            val derived = yamlLimits(relaxed) { maximumDepth = DERIVED_DEPTH }

            relaxed.maximumQueryNesting shouldBe YamlLimits.HARD_QUERY_NESTING
            derived.maximumQuerySteps shouldBe relaxed.maximumQuerySteps
            derived.maximumPatternInputLength shouldBe relaxed.maximumPatternInputLength
            derived.toString() shouldContain "querySteps=${relaxed.maximumQuerySteps}"
            derived.hashCode() shouldBe yamlLimits(relaxed) { maximumDepth = DERIVED_DEPTH }.hashCode()
        }
    }
})
