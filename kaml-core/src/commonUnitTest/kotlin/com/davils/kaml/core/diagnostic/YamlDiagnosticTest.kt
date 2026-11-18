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
package com.davils.kaml.core.diagnostic

import com.davils.kaml.core.fixture.everyHostileFragment
import com.davils.kaml.core.fixture.hostileText
import com.davils.kaml.core.fixture.shouldBeSafeToShow
import com.davils.kore.core.Limits
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.checkAll

private const val MESSAGE: String = "The key appears twice"
private const val LONG_MESSAGE: Int = 10_000
private const val SHIFT: Int = 5

private val range: TextRange = TextRange(start = 0, end = 1)

class YamlDiagnosticTest : FunSpec({
    val diagnostic = YamlDiagnostic(code = YamlDiagnosticCode.DuplicateKey, range = range, message = MESSAGE)

    context("a diagnostic") {
        test("takes the severity of its code by default") {
            diagnostic.severity shouldBe Severity.Error
            YamlDiagnostic(YamlDiagnosticCode.ReservedDirective, range, MESSAGE).severity shouldBe Severity.Warning
        }

        test("keeps a severity it is given") {
            val warning = YamlDiagnostic(
                code = YamlDiagnosticCode.DuplicateKey,
                range = range,
                message = MESSAGE,
                severity = Severity.Warning,
            )

            warning.severity shouldBe Severity.Warning
        }

        test("keeps a safe message as it is") {
            diagnostic.message shouldBe MESSAGE
            diagnostic.code shouldBe YamlDiagnosticCode.DuplicateKey
            diagnostic.range shouldBe range
        }

        test("escapes a hostile message") {
            YamlDiagnostic(YamlDiagnosticCode.DuplicateKey, range, everyHostileFragment).message.shouldBeSafeToShow()
            checkAll(hostileText) { text ->
                val hostile = YamlDiagnostic(YamlDiagnosticCode.DuplicateKey, range, "The key $text appears twice")

                hostile.message.shouldBeSafeToShow()
                hostile.toString().shouldBeSafeToShow()
            }
        }

        test("shortens a long message") {
            val long = YamlDiagnostic(YamlDiagnosticCode.DuplicateKey, range, "x".repeat(LONG_MESSAGE))

            (long.message.length < LONG_MESSAGE) shouldBe true
            long.message shouldStartWith "x".repeat(Limits.MAX_MESSAGE_LENGTH)
        }

        test("moves to another range with the same message") {
            val long = YamlDiagnostic(YamlDiagnosticCode.DuplicateKey, range, "x".repeat(LONG_MESSAGE))
            val moved = long.movedTo(TextRange(start = SHIFT, end = SHIFT))

            moved.message shouldBe long.message
            moved.range shouldBe TextRange(start = SHIFT, end = SHIFT)
            moved.code shouldBe long.code
            moved.severity shouldBe long.severity
        }
    }

    context("equality") {
        test("compares every property") {
            val same = YamlDiagnostic(code = YamlDiagnosticCode.DuplicateKey, range = range, message = MESSAGE)
            val others = listOf(
                YamlDiagnostic(code = YamlDiagnosticCode.UnknownTag, range = range, message = MESSAGE),
                YamlDiagnostic(code = YamlDiagnosticCode.DuplicateKey, range = TextRange.at(SHIFT), message = MESSAGE),
                YamlDiagnostic(code = YamlDiagnosticCode.DuplicateKey, range = range, message = "Another message"),
                YamlDiagnostic(
                    code = YamlDiagnosticCode.DuplicateKey,
                    range = range,
                    message = MESSAGE,
                    severity = Severity.Warning,
                ),
            )
            val anything: Any = diagnostic

            diagnostic shouldBe same
            diagnostic.hashCode() shouldBe same.hashCode()
            others.forEach { other -> diagnostic shouldNotBe other }
            anything shouldNotBe MESSAGE
        }
    }

    context("the text form") {
        test("names the code, the severity, the range and the message") {
            diagnostic.toString() shouldBe
                "YamlDiagnostic(code=kaml.mapping.duplicate-key, severity=Error, range=0..1, message=$MESSAGE)"
        }
    }

    context("the codes") {
        test("have distinct values in their groups") {
            val values = YamlDiagnosticCode.entries.map(YamlDiagnosticCode::value)

            values.toSet() shouldHaveSize values.size
            values.all { value -> value.startsWith("kaml.") } shouldBe true
            YamlDiagnosticCode.TooDeep.toString() shouldBe "kaml.limit.depth"
            YamlDiagnosticCode.InvalidAnchor.toString() shouldContain "anchor"
        }
    }
})
