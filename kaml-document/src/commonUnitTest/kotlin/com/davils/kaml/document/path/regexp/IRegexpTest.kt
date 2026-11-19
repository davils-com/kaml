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

package com.davils.kaml.document.path.regexp

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.fixture.timed
import com.davils.kaml.document.internal.WorkBudget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val PLENTY: Long = 1_000_000_000L
private const val LONG_INPUT: Int = 20_000
private const val SMALL_PROGRAM: Int = 10
private const val SHALLOW: Int = 3
private const val AT_LEAST_TWO: String = "a{2,}"
private const val LAST_BMP: Int = 0xFFFF
private const val STEPS: String = "maximumQuerySteps"
private const val PATHOLOGICAL: String = "(a*)*b"
private const val REPEATED: Int = 100
private const val TINY_BUDGET: Long = 10L
private const val TINIER_BUDGET: Long = 5L
private const val GROWTH: Long = 3L
private const val STEPS_PER_CHARACTER: Int = 64
private const val ALTERNATING: String = "(a|b)*c"
private const val RUNS: Int = 100_000
private const val RUN_GROWTH: Int = 10
private val slack: Duration = 200.milliseconds

private val valid: List<String> = listOf(
    "", "a", "a|b", "|", "(a)", "()", "a*", "a+", "a?", "b{2}", "b{2,}", "b{2,3}", "b{0}", "a{01}", "[abc]",
    "[^abc]", "[a-z]", "[-a]", "[a-]", "[-]", "[--]", "[^-]", "[\\-]", "[a\\]]", "[\\p{Nd}y]", "[\\P{L}]",
    "\\p{L}", "\\P{Lu}", ".", "\\.", "\\n", "\\r", "\\t", "\\\\", "\\^", "\\{", "\\}", "\\|", "\\(", "\\)",
    "\\[", "\\]", "\\*", "\\+", "\\?", "\\-", "$", "^", ",", "-", "😀", "a{2}b{0,1}(c|d)*",
)

private val invalid: List<String> = listOf(
    "(", ")", "a)", "a**", "*", "+a", "?", "{", "}", "a{", "a{2", "a{3,2}", "a{,2}", "a{2}{3}", "[", "[]", "[^]",
    "]", "[a", "[a-\\p{L}]", "[\\p{L}-z]", "[z-a]", "[a-z-0]", "[[]", "\\", "\\d", "\\w", "\\s", "\\b", "\\1",
    "(?:a)", "(?=a)", "\\S", "\\D", "\\W", "[\\S ]", "\\p{IsBasicLatin}",
    "[\\u0000-\\u007f]", "\\p{X}", "\\p{Cs}", "\\p{Lx}", "\\p{L", "\\p", "\\pL", "a\uD800", "[\uDC00]",
)

private val members: List<Pair<String, Int>> = listOf(
    "L" to 'a'.code, "Lu" to 'A'.code, "Ll" to 'a'.code, "Lt" to 0x01C5, "Lm" to 0x02B0, "Lo" to 0x05D0,
    "Lo" to 0x65E5, "M" to 0x0301, "Mn" to 0x0301, "Mc" to 0x0903, "Me" to 0x20DD, "N" to '7'.code, "Nd" to '7'.code,
    "Nl" to 0x216B,
    "No" to 0x00B2, "P" to '!'.code, "Pc" to '_'.code, "Pd" to '-'.code, "Ps" to '('.code, "Pe" to ')'.code,
    "Pi" to 0x00AB, "Pf" to 0x00BB, "Po" to '!'.code, "S" to '+'.code, "Sm" to '+'.code, "Sc" to '$'.code,
    "Sk" to '^'.code, "So" to 0x00A9, "Z" to ' '.code, "Zs" to ' '.code, "Zl" to 0x2028, "Zp" to 0x2029,
    "C" to 0x0000, "Cc" to 0x0007, "Cf" to 0x00AD, "Cn" to 0x0378, "C" to 0xD800, "So" to 0x1F600,
)

private val strangers: List<Pair<String, Int>> = listOf(
    "L" to '1'.code,
    "Lu" to 'a'.code,
    "Cc" to 'a'.code,
    "L" to 0x1F600,
)

private fun compiled(pattern: String, limits: YamlLimits = YamlLimits.Untrusted): IRegexp? =
    IRegexp.compile(pattern, limits, WorkBudget(PLENTY, STEPS))

private fun isIn(category: String, codePoint: Int): Boolean {
    val subject = "😀".takeIf { codePoint > LAST_BMP } ?: Char(codePoint).toString()
    val isIncluded = "\\p{$category}" matchesWhole subject
    val isExcluded = "\\P{$category}" matchesWhole subject
    return isIncluded == true && isExcluded == false
}

private infix fun String.matchesWhole(subject: String): Boolean? {
    val regexp = checkNotNull(compiled(this))
    return regexp.matches(subject, WorkBudget(PLENTY, STEPS))
}

private infix fun String.searchesIn(subject: String): Boolean? {
    val regexp = checkNotNull(compiled(this))
    return regexp.containsMatchIn(subject, WorkBudget(PLENTY, STEPS))
}

private fun plenty(): WorkBudget = WorkBudget(PLENTY, STEPS)

private infix fun String.stepsFor(subject: String): Long {
    val budget = WorkBudget(PLENTY, STEPS)
    checkNotNull(compiled(this)).matches(subject, budget)
    return budget.spent
}

class IRegexpTest : FunSpec({
    context("the grammar of RFC 9485") {
        test("accepts every valid pattern") {
            valid.filter { pattern -> compiled(pattern) == null }.shouldBeEmpty()
        }

        test("refuses backreferences, lookaround, shorthand classes and every other invalid pattern") {
            invalid.filter { pattern -> compiled(pattern) != null }.shouldBeEmpty()
        }
    }

    context("matching") {
        test("tests the whole text with match and any part with search") {
            "b" matchesWhole "abc" shouldBe false
            "b" searchesIn "abc" shouldBe true
            "a.c" matchesWhole "abc" shouldBe true
            "" matchesWhole "" shouldBe true
            "" searchesIn "abc" shouldBe true
            "x" searchesIn "" shouldBe false
            "c(a|o)t" searchesIn "the cot" shouldBe true
            "cat|dog" matchesWhole "dog" shouldBe true
            "cat|dog" matchesWhole "cow" shouldBe false
        }

        test("lets a dot match any code point but a line feed or a carriage return") {
            "a.c" matchesWhole "a\nc" shouldBe false
            "a.c" matchesWhole "a\rc" shouldBe false
            "a.c" matchesWhole "a\tc" shouldBe true
            "." matchesWhole "😀" shouldBe true
            ".." matchesWhole "😀" shouldBe false
            "[^a]" matchesWhole "\n" shouldBe true
        }

        test("reads classes, ranges and negations") {
            "[a-c]+" matchesWhole "abcabc" shouldBe true
            "[^a-c]" matchesWhole "d" shouldBe true
            "[^a-c]" matchesWhole "a" shouldBe false
            "[-a]" matchesWhole "-" shouldBe true
            "[a-]" matchesWhole "-" shouldBe true
            "[\\-]" matchesWhole "-" shouldBe true
            "[a\\]]" matchesWhole "]" shouldBe true
            "[😀-😂]" matchesWhole "😁" shouldBe true
            "[\\n]" matchesWhole "\n" shouldBe true
            "\\t" matchesWhole "\t" shouldBe true
            "[\\p{Nd}x]" matchesWhole "x" shouldBe true
            "[\\p{Nd}x]" matchesWhole "y" shouldBe false
            "$" matchesWhole "$" shouldBe true
            "^" matchesWhole "^" shouldBe true
        }

        test("repeats exactly, at least and within bounds") {
            "a{3}" matchesWhole "aaa" shouldBe true
            "a{3}" matchesWhole "aa" shouldBe false
            "a{3}" matchesWhole "aaaa" shouldBe false
            AT_LEAST_TWO matchesWhole "aa" shouldBe true
            AT_LEAST_TWO matchesWhole "aaaaa" shouldBe true
            AT_LEAST_TWO matchesWhole "a" shouldBe false
            "a{2,3}" matchesWhole "aaa" shouldBe true
            "a{2,3}" matchesWhole "aaaa" shouldBe false
            "a{0}" matchesWhole "" shouldBe true
            "(ab){2}" matchesWhole "abab" shouldBe true
            "a?b+" matchesWhole "bbb" shouldBe true
            "a?b+" matchesWhole "aab" shouldBe false
        }

        test("loops over groups that match nothing") {
            "(a*)*" matchesWhole "aaaa" shouldBe true
            "(a*)*" matchesWhole "" shouldBe true
            "(a|)*b" matchesWhole "aab" shouldBe true
            "()*" matchesWhole "" shouldBe true
        }
    }

    context("Unicode categories") {
        test("give the same answer on every platform") {
            val wrongMembers = members.filter { (category, codePoint) -> !isIn(category, codePoint) }
            val wrongStrangers = strangers.filter { (category, codePoint) -> isIn(category, codePoint) }

            wrongMembers.shouldBeEmpty()
            wrongStrangers.shouldBeEmpty()
        }
    }

    context("limits") {
        test("refuse a pattern longer than its limit") {
            val limits = yamlLimits { maximumRegexpLength = 4 }

            compiled("abcd", limits).shouldNotBeNull()
            compiled("abcde", limits).shouldBeNull()
        }

        test("refuse a program larger than its limit") {
            val limits = yamlLimits { maximumRegexpSize = SMALL_PROGRAM }

            compiled("a{9}", limits).shouldNotBeNull()
            compiled("a{10}", limits).shouldBeNull()
            compiled("a{99999999999}", limits).shouldBeNull()
            compiled("((a{1000}){1000}){1000}").shouldBeNull()
        }

        test("refuse a pattern that repeats empty groups more often than the program size allows") {
            val limits = yamlLimits { maximumRegexpSize = SMALL_PROGRAM }

            compiled("((){4}){3}", limits).shouldNotBeNull()
            compiled("a{10}bc", limits).shouldBeNull()
            compiled("((){4}){4}", limits).shouldBeNull()
            compiled("((){1000}){1000}").shouldBeNull()
            compiled("(((){1000}){1000}){1000}").shouldBeNull()
            compiled("(ab){4000}").shouldNotBeNull()
        }

        test("charge every node the compiler emits against the budget") {
            val exceeded = shouldThrow<YamlDocumentLimitException> {
                IRegexp.compile("(){9}", YamlLimits.Untrusted, WorkBudget(TINIER_BUDGET, STEPS))
            }

            exceeded.limit shouldBe STEPS
        }

        test("refuse groups nested deeper than the limit") {
            val limits = yamlLimits { maximumQueryNesting = SHALLOW }

            compiled("(((a)))", limits).shouldNotBeNull()
            compiled("((((a))))", limits).shouldBeNull()
            compiled("(".repeat(LONG_INPUT) + ")".repeat(LONG_INPUT)).shouldBeNull()
        }

        test("stop when the budget runs out") {
            val regexp = checkNotNull(compiled(ALTERNATING))

            val subject = "ab".repeat(REPEATED)

            regexp.matches(subject, WorkBudget(TINY_BUDGET, STEPS)).shouldBeNull()
            regexp.containsMatchIn(subject, WorkBudget(TINY_BUDGET, STEPS)).shouldBeNull()
            regexp.matches(subject, WorkBudget(TINIER_BUDGET, STEPS)).shouldBeNull()
        }
    }

    context("ranges") {
        test("match only the code points between their bounds") {
            val range = "[b-c]"

            range matchesWhole "a" shouldBe false
            range matchesWhole "c" shouldBe true
            range matchesWhole "d" shouldBe false
        }
    }

    context("repeated runs") {
        test("give the same answers as a fresh program") {
            val reused = checkNotNull(compiled(ALTERNATING))
            val subjects = listOf("abc", "", "c", "abab", "x", "bbbc", "ab")

            subjects.forEach { subject ->
                val fresh = checkNotNull(compiled(ALTERNATING))
                reused.matches(subject, plenty()) shouldBe fresh.matches(subject, plenty())
                reused.containsMatchIn(subject, plenty()) shouldBe fresh.containsMatchIn(subject, plenty())
            }
        }

        test("cost no more for a large program than for a small one on short subjects") {
            val small = checkNotNull(compiled("ab"))
            val large = checkNotNull(compiled("ab{9990}"))
            val smallTime = timed { repeat(RUNS) { small.matches("", plenty()) } }
            val largeTime = timed { repeat(RUNS) { large.matches("", plenty()) } }

            (largeTime < smallTime * RUN_GROWTH + slack) shouldBe true
        }
    }

    context("pathological patterns") {
        val long = "a".repeat(LONG_INPUT)

        test("finish in linear time") {
            PATHOLOGICAL matchesWhole long shouldBe false
            "(a|a)*b" matchesWhole long shouldBe false
            "(a+)+$" matchesWhole "$long!" shouldBe false
            "(.*a){20}" matchesWhole long shouldBe true
            PATHOLOGICAL searchesIn long shouldBe false
            "(a|aa)*c" matchesWhole long shouldBe false
        }

        test("take steps in proportion to the input") {
            val short = PATHOLOGICAL stepsFor "a".repeat(LONG_INPUT / 2)
            val twice = PATHOLOGICAL stepsFor long

            twice shouldBeLessThan short * GROWTH
            (twice / LONG_INPUT).toInt() shouldBeLessThan STEPS_PER_CHARACTER
        }
    }
})
