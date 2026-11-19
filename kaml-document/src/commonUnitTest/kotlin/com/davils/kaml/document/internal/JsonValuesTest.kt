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

package com.davils.kaml.document.internal

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.node.yamlTagged
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.fixture.collidingMapping
import com.davils.kaml.document.fixture.growsNearLinearly
import com.davils.kaml.document.fixture.nodeOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val LARGE: Long = 1_000_000_000L
private const val STEPS: String = "maximumQuerySteps"
private const val SHALLOW: Int = 10
private const val ITEMS: Int = 100
private const val NEEDED_STEPS: Long = 101L
private const val TWO_TO_THE_53: String = "9007199254740992"
private const val PAST_TWO_TO_THE_53: String = "9007199254740993"
private const val ONE: String = "1"
private const val ONE_MEMBER: String = "{a: 1}"
private const val ZEROS: Int = 30
private const val MANY_ZEROS: Int = 400
private const val LONE_HIGH: Int = 0xD800
private const val TEN_TO_THE_30: String = "1000000000000000019884624838656"
private const val THIRTY_DIGITS: String = "123456789012345678901234567890"
private const val HUGE: Double = 1.0E308
private const val HUGE_EXPONENT: Int = 308
private const val LEADING_DIGITS: Int = 17
private const val TRAILING_DIGITS: Int = 7

private fun number(text: String): NumberValue = checkNotNull(NumberValue.of(yamlTagged(text, YamlTag.INTEGER)))

private fun real(text: String): NumberValue = real(text.toDouble())

private fun real(value: Double): NumberValue = checkNotNull(NumberValue.of(yamlScalar(value)))

private fun steps(): WorkBudget = WorkBudget(LARGE, STEPS)

private fun equality(steps: Long = LARGE, depth: Int = SHALLOW): JsonEquality =
    JsonEquality(WorkBudget(steps, STEPS), depth)

private infix fun String.equalsJson(other: String): Boolean = equality().isEqual(
    left = nodeOf(this),
    right = nodeOf(other),
)

private infix fun String.isLessJson(other: String): Boolean = equality().isLess(
    left = nodeOf(this),
    right = nodeOf(other),
)

private infix fun String.comparedTo(other: String): Int = CodePoints.compare(left = this, right = other)

private fun nested(depth: Int): YamlNode =
    (1..depth).fold<Int, YamlNode>(yamlScalar("x")) { node, _ -> yamlSequence(listOf(node)) }

private fun empties(): YamlNode = yamlSequence(List(ITEMS) { yamlScalar("") })

class JsonValuesTest : FunSpec({
    context("exact numbers") {
        test("compare integers by their canonical decimal, not as doubles") {
            number(PAST_TWO_TO_THE_53).compareTo(number(TWO_TO_THE_53), steps()) shouldBe 1
            number("-$PAST_TWO_TO_THE_53").compareTo(number("-$TWO_TO_THE_53"), steps()) shouldBe -1
            number(THIRTY_DIGITS).compareTo(number(THIRTY_DIGITS), steps()) shouldBe 0
            number("+01").compareTo(number(ONE), steps()) shouldBe 0
            number("-0").compareTo(number("0"), steps()) shouldBe 0
        }

        test("compare an integer with a real exactly") {
            number(PAST_TWO_TO_THE_53).compareTo(real("9.007199254740992E15"), steps()) shouldBe 1
            real("9.007199254740992E15").compareTo(number(PAST_TWO_TO_THE_53), steps()) shouldBe -1
            number(ONE).compareTo(real("1.0"), steps()) shouldBe 0
            number(ONE).compareTo(real("1.5"), steps()) shouldBe -1
            number("2").compareTo(real("1.5"), steps()) shouldBe 1
            number("-2").compareTo(real("-1.5"), steps()) shouldBe -1
            number(ONE).compareTo(real(Double.POSITIVE_INFINITY), steps()) shouldBe -1
            number(ONE).compareTo(real(Double.NEGATIVE_INFINITY), steps()) shouldBe 1
            number(ONE).compareTo(real(Double.NaN), steps()) shouldBe null
        }

        test("compare integers beyond the range of a long with large reals") {
            number("1" + "0".repeat(ZEROS)).compareTo(real("1.0E30"), steps()) shouldBe -1
            number(TEN_TO_THE_30).compareTo(real("1.0E30"), steps()) shouldBe 0
            number("-$TEN_TO_THE_30").compareTo(real("-1.0E30"), steps()) shouldBe 0
            number("1" + "0".repeat(MANY_ZEROS)).compareTo(real(Double.MAX_VALUE), steps()) shouldBe 1
            number("1" + "0".repeat(HUGE_EXPONENT)).compareTo(real(HUGE), steps()) shouldBe -1
            number("2" + "0".repeat(HUGE_EXPONENT)).compareTo(real(HUGE), steps()) shouldBe 1
            number("-" + "1" + "0".repeat(HUGE_EXPONENT)).compareTo(real(-HUGE), steps()) shouldBe 1
        }

        test("compare an integer with a real of another magnitude without expanding the real") {
            val oneStep = WorkBudget(1L, STEPS)

            number("1999").compareTo(real(HUGE), oneStep) shouldBe -1
            number("-1999").compareTo(real(-HUGE), oneStep) shouldBe 1
            number("-1999").compareTo(real(HUGE), oneStep) shouldBe -1
            number("1999").compareTo(real(-HUGE), oneStep) shouldBe 1
            number("1" + "0".repeat(MANY_ZEROS)).compareTo(real(HUGE), oneStep) shouldBe 1
            oneStep.spent shouldBe 0L
        }

        test("charge the expansion of a real of the same magnitude") {
            val exceeded = shouldThrow<YamlDocumentLimitException> {
                number("1" + "0".repeat(HUGE_EXPONENT)).compareTo(real(HUGE), WorkBudget(1L, STEPS))
            }

            exceeded.limit shouldBe STEPS
        }

        test("compare reals as IEEE numbers") {
            real("-0.0").compareTo(real("0.0"), steps()) shouldBe 0
            real("1.0").compareTo(real("2.0"), steps()) shouldBe -1
            real("2.0").compareTo(real("1.0"), steps()) shouldBe 1
            real(Double.NaN).compareTo(real(Double.NaN), steps()) shouldBe null
            real(Double.NaN).compareTo(number(ONE), steps()) shouldBe null
            real("1.0").compareTo(real(Double.NaN), steps()) shouldBe null
            NumberValue.of(yamlScalar(2L))?.compareTo(number("2"), steps()) shouldBe 0
        }

        test("fall back to the double of an integer it cannot read as a decimal") {
            number("0x10").compareTo(real(Double.NaN), steps()) shouldBe null
            NumberValue.of(yamlScalar(ONE)) shouldBe null
        }
    }

    context("decimals") {
        test("canonicalize signs, separators and leading zeros") {
            Decimals.canonical("-000") shouldBe "0"
            Decimals.canonical("+0012") shouldBe "12"
            Decimals.canonical("-1_000") shouldBe "-1000"
            Decimals.canonical("1e3") shouldBe null
            Decimals.canonical("-") shouldBe null
            Decimals.canonical("") shouldBe null
        }

        test("write integral doubles exactly") {
            Decimals.ofIntegral("-3".toDouble()) shouldBe "-3"
            Decimals.ofIntegral("1.0E19".toDouble()) shouldBe "10000000000000000000"
            Decimals.ofIntegral("-1.0E19".toDouble()) shouldBe "-10000000000000000000"
            Decimals.ofIntegral("9.223372036854775808E18".toDouble()) shouldBe "9223372036854775808"
            val largest = Decimals.ofIntegral(Double.MAX_VALUE)
            largest.length shouldBe HUGE_EXPONENT + 1
            largest.take(LEADING_DIGITS) shouldBe "17976931348623157"
            largest.takeLast(TRAILING_DIGITS) shouldBe "4858368"
        }
    }

    context("code points") {
        test("compare by code point, not by UTF-16 unit") {
            "\uFFFF" comparedTo "😀" shouldBe -1
            "a" comparedTo "ab" shouldBe -1
            "ab" comparedTo "a" shouldBe 1
            "ab" comparedTo "ab" shouldBe 0
        }

        test("count code points and keep lone surrogates") {
            CodePoints.count("a😀") shouldBe 2
            CodePoints.of("\uD800a").toList() shouldBe listOf(LONE_HIGH, 'a'.code)
        }
    }

    context("equality") {
        test("compares in the sense of JSON") {
            "{a: [1, 2.0], b: x}" equalsJson "{b: x, a: [1.0, 2]}" shouldBe true
            "[1]" equalsJson "[1, 2]" shouldBe false
            ONE_MEMBER equalsJson "{b: 1}" shouldBe false
            ONE equalsJson "'1'" shouldBe false
            "true" equalsJson "true" shouldBe true
            "true" equalsJson "false" shouldBe false
            "true" equalsJson ONE shouldBe false
            ONE equalsJson "2" shouldBe false
            "~" equalsJson "null" shouldBe true
            "[1]" equalsJson ONE_MEMBER shouldBe false
            ONE_MEMBER equalsJson "[1]" shouldBe false
        }

        test("orders only numbers with numbers and texts with texts") {
            ONE isLessJson "2.5" shouldBe true
            "a" isLessJson "b" shouldBe true
            ONE isLessJson "'2'" shouldBe false
            "[1]" isLessJson "[2]" shouldBe false
        }

        test("compares every member of keys with the same text, in their order") {
            val first = YamlEntry(key = yamlScalar(1), value = yamlScalar(1))
            val second = YamlEntry(key = yamlScalar(ONE), value = yamlScalar(2))
            val sameText = yamlMapping(listOf(first, second))
            val other = yamlMapping(listOf(first, YamlEntry(key = yamlScalar(ONE), value = yamlScalar(1))))

            equality().isEqual(left = sameText, right = yamlMapping(listOf(first, second))) shouldBe true
            equality().isEqual(left = sameText, right = nodeOf("{'1': 1}")) shouldBe false
            equality().isEqual(left = sameText, right = other) shouldBe false
            equality().isEqual(left = sameText, right = nodeOf("{a: 1, b: 2}")) shouldBe false
            equality().isEqual(left = nodeOf("{a: 1, b: 2}"), right = nodeOf("{a: 1, c: 2}")) shouldBe false
            equality().isEqual(left = nodeOf("{1: x, '1': y, b: z}"), right = nodeOf("{1: x, b: z, !t b: w}")) shouldBe
                false
            equality().isEqual(left = yamlNull(), right = yamlNull()) shouldBe true
        }

        test("compares mappings in near-linear time, also when the hash codes of their keys collide") {
            val isLinear = growsNearLinearly { bits ->
                equality().isEqual(left = collidingMapping(bits), right = collidingMapping(bits)) shouldBe true
            }

            isLinear shouldBe true
        }

        test("charges its steps and stops at the budget") {
            equality(steps = NEEDED_STEPS).isEqual(left = empties(), right = empties()) shouldBe true
            val exceeded = shouldThrow<YamlDocumentLimitException> {
                equality(steps = NEEDED_STEPS - 1).isEqual(left = empties(), right = empties())
            }

            exceeded.limit shouldBe STEPS
        }

        test("refuses to descend beyond its depth") {
            equality().isEqual(left = nested(SHALLOW), right = nested(SHALLOW)) shouldBe true
            val exceeded = shouldThrow<YamlDocumentLimitException> {
                equality().isEqual(left = nested(SHALLOW + 1), right = nested(SHALLOW + 1))
            }

            exceeded.limit shouldBe "maximumDepth"
        }
    }

    context("node sets") {
        test("tell nodes apart by identity") {
            val node = yamlScalar("a")
            val nodes = NodeSet(identityOrdinals())

            nodes.add(node) shouldBe true
            nodes.add(node) shouldBe false
            (yamlScalar("a") in nodes) shouldBe false
            nodes.add(yamlScalar("a")) shouldBe true
            nodes.nodes.size shouldBe 2
        }
    }
})
