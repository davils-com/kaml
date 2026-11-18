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

private const val TOO_DEEP: Int = YamlLimits.HARD_DEPTH + 1

class YamlLimitsConstructionTest : FunSpec({
    context("limits created through their constructor") {
        test("are refused beyond a hard ceiling") {
            val builder = YamlLimitsBuilder()
            builder.maximumDepth = TOO_DEEP
            shouldThrow<ValidationException> { YamlLimits(builder) }
        }

        test("are refused below one") {
            val builder = YamlLimitsBuilder()
            builder.maximumInputLength = 0
            shouldThrow<ValidationException> { YamlLimits(builder) }
        }

        test("are created within their bounds") {
            YamlLimits(YamlLimitsBuilder()) shouldBe YamlLimits.Untrusted
        }
    }
})
