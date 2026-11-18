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

import com.davils.kaml.core.YamlViolationCodes
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val SMALL: Int = 7

private fun codesOf(error: ValidationException): List<String> = error.report.violations.map { it.code.value }

class YamlLimitsTest : FunSpec({
    context("every limit") {
        limitAccesses.forEach { limit ->
            test("${limit.name} accepts its hard ceiling") {
                val built = yamlLimits { limit.assignTo(this, limit.ceiling) }

                limit.of(built) shouldBe limit.ceiling
            }

            test("${limit.name} refuses one past its hard ceiling") {
                val error = shouldThrow<ValidationException> { yamlLimits { limit.assignTo(this, limit.ceiling + 1) } }

                codesOf(error) shouldContain YamlViolationCodes.ABOVE_HARD_LIMIT.value
            }

            test("${limit.name} accepts 1 and refuses 0") {
                limit.of(yamlLimits { limit.assignTo(this, 1L) }) shouldBe 1L
                val error = shouldThrow<ValidationException> { yamlLimits { limit.assignTo(this, 0L) } }

                codesOf(error) shouldContain YamlViolationCodes.NOT_POSITIVE.value
            }

            test("${limit.name} keeps its value in the relaxed limits within its ceiling") {
                val relaxed = limit.of(YamlLimits.Relaxed)

                (relaxed <= limit.ceiling) shouldBe true
                (relaxed >= limit.of(YamlLimits.Untrusted)) shouldBe true
            }
        }
    }

    context("the presets") {
        test("start from the defaults of the builder") {
            YamlLimits.Untrusted.maximumInputLength shouldBe YamlLimitsBuilder.DEFAULT_INPUT_LENGTH
            YamlLimits.Untrusted.maximumDepth shouldBe YamlLimitsBuilder.DEFAULT_DEPTH
            YamlLimits.Untrusted.maximumNodes shouldBe YamlLimitsBuilder.DEFAULT_NODES
            YamlLimits.Untrusted.maximumAliasExpansion shouldBe YamlLimitsBuilder.DEFAULT_ALIAS_EXPANSION
            YamlLimits.Untrusted.maximumAnchors shouldBe YamlLimitsBuilder.DEFAULT_ANCHORS
            YamlLimits.Untrusted.maximumScalarLength shouldBe YamlLimitsBuilder.DEFAULT_SCALAR_LENGTH
            YamlLimits.Untrusted.maximumCollectionSize shouldBe YamlLimitsBuilder.DEFAULT_COLLECTION_SIZE
            YamlLimits.Untrusted.maximumDocuments shouldBe YamlLimitsBuilder.DEFAULT_DOCUMENTS
            YamlLimits.Untrusted.maximumNumberLength shouldBe YamlLimitsBuilder.DEFAULT_NUMBER_LENGTH
            YamlLimits.Untrusted.maximumDiagnostics shouldBe YamlLimitsBuilder.DEFAULT_DIAGNOSTICS
            YamlLimits.Untrusted.maximumOutputLength shouldBe YamlLimitsBuilder.DEFAULT_OUTPUT_LENGTH
        }

        test("relax the depth to its hard ceiling") {
            YamlLimits.Relaxed.maximumDepth shouldBe YamlLimits.HARD_DEPTH
            YamlLimits.Relaxed.maximumNumberLength shouldBe YamlLimits.HARD_NUMBER_LENGTH
        }
    }

    context("equality") {
        test("compares every limit") {
            val anything: Any = YamlLimits.Untrusted

            yamlLimits { } shouldBe YamlLimits.Untrusted
            yamlLimits { }.hashCode() shouldBe YamlLimits.Untrusted.hashCode()
            YamlLimits.Relaxed shouldNotBe YamlLimits.Untrusted
            limitAccesses.forEach { limit -> yamlLimits { limit.assignTo(this, 1L) } shouldNotBe YamlLimits.Untrusted }
            anything shouldNotBe YamlLimits.Untrusted.toString()
        }

        test("starts from a base") {
            val based = yamlLimits(YamlLimits.Relaxed) { maximumDepth = SMALL }

            based.maximumDepth shouldBe SMALL
            based.maximumNodes shouldBe YamlLimits.Relaxed.maximumNodes
        }
    }

    context("the text form") {
        test("names every limit") {
            YamlLimits.Untrusted.toString() shouldBe
                "YamlLimits(input=67108864, depth=256, nodes=5000000, aliasExpansion=100000, anchors=10000, " +
                "scalar=16777216, collection=1000000, documents=10000, number=1000, diagnostics=1000, " +
                "output=67108864)"
        }
    }

    context("the builder") {
        test("refuses a change after the block was built") {
            var leaked: YamlLimitsBuilder? = null
            yamlLimits { leaked = this }

            limitAccesses.forEach { limit ->
                shouldThrow<IllegalStateException> { leaked?.let { builder -> limit.assignTo(builder, 1L) } }
            }
        }

        test("names the limits it builds") {
            val error = shouldThrow<ValidationException> { yamlLimits { maximumDepth = 0 } }

            error.message shouldContain "maximumDepth"
        }
    }
})
