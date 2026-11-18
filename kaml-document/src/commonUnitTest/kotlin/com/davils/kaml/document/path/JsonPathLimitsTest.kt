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

package com.davils.kaml.document.path

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.YamlLimitsBuilder
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val DEEP: Int = 100_000
private const val NESTING: Int = 4
private const val DEPTH: Int = 1_000
private const val WIDTH: Int = 50_000
private const val ITEMS: Int = 100
private const val RESULTS: Int = 3
private const val KEY: Int = 7
private const val LEVELS: Int = 3
private const val SUBJECT_LENGTH: Int = 10_000
private const val SMALL_BUDGET: Long = 1_000L
private const val PIECES: Int = 32
private const val MOST_FRAGMENTS: Int = 24
private const val MOST_STEPS: Long = 1_000_000L
private const val TIGHT_BUDGET: Long = 2_000L
private const val STEPS: String = "maximumQuerySteps"
private const val DESCENDANTS: String = "$..*"
private const val SAME_AS_SECOND: String = "$[?@ == $[1]]"
private const val FEW_OPERANDS: Int = 10
private const val MANY_OPERANDS: Int = 100
private const val LEVELS_DEEP: Int = 50
private const val INTEGERS: Int = 2_000
private const val GROWTH: Int = 10
private val slack: Duration = 200.milliseconds

private infix fun String.selectedIn(node: YamlNode): List<JsonPathMatch> = JsonPath.parse(this).query(node)

private fun String.selectedIn(node: YamlNode, limits: YamlLimits): List<JsonPathMatch> {
    val path = JsonPath.parse(this, limits)
    return path.query(node)
}

private fun messageOf(failing: () -> Unit): String {
    val failure = runCatching(failing).exceptionOrNull()
    return failure?.message.orEmpty()
}

private val fragments: List<String> = listOf(
    "$", "@", ".", "..", "[", "]", "?", "(", ")", "!", "&&", "||", "==", "!=", "<", "<=", ">", "'", "\"", "*", ":",
    ",", "0", "1", "-", "e", "E", "+", "a", "_", "length(", "count(", "match(", "search(", "value(", " ", "\\",
    "\\u", "D800", "true", "false", "null", "1e999", "9007199254740992", ".5", "😀",
) +
    HostileFragments.all

private val expressions: Arb<String> = Arb.list(Arb.element(fragments), 0..MOST_FRAGMENTS).map { it.joinToString("") }

private val document: YamlNode = nodeOf("{a: [1, 2.5, x, {b: [true, null, 'y']}], c: {d: 'TOKEN'}, e: ''}")

private fun limits(configure: YamlLimitsBuilder.() -> Unit): YamlLimits = yamlLimits(YamlLimits.Untrusted, configure)

private fun nested(depth: Int): YamlNode =
    (1..depth).fold<Int, YamlNode>(yamlScalar("x")) { node, _ -> yamlSequence(listOf(node)) }

private fun conjunction(operands: Int): String = "$[?" + List(operands) { "@" }.joinToString("&&") + "]"

private fun timed(work: () -> Unit): Duration {
    val mark = TimeSource.Monotonic.markNow()
    work()
    return mark.elapsedNow()
}

private fun minimalSteps(expression: String, node: YamlNode): Long {
    var low = 1L
    var high = MOST_STEPS
    while (low < high) {
        val middle = (low + high) / 2
        val path = JsonPath.parse(expression, limits { maximumQuerySteps = middle })
        val isEnough = runCatching { path.query(node) }.isSuccess
        if (isEnough) high = middle
        if (!isEnough) low = middle + 1
    }
    return low
}

class JsonPathLimitsTest : FunSpec({
    context("parsing") {
        test("refuses an expression longer than its limit") {
            val short = limits { maximumQueryLength = 5 }

            JsonPath.parseOrNull("$.abc", short).shouldNotBeNull()
            JsonPath.parseOrNull("$.abcd", short).shouldBeNull()
            shouldThrow<IllegalArgumentException> { JsonPath.parse("$.abcd", short) }.message shouldBe
                "\"$.abcd\" is not a JSONPath query: the expression is too long at offset 0"
        }

        test("refuses nesting deeper than its limit") {
            val shallow = limits { maximumQueryNesting = NESTING }

            JsonPath.parseOrNull("$[?(((@)))]", shallow).shouldNotBeNull()
            JsonPath.parseOrNull("$[?((((@))))]", shallow).shouldBeNull()
            JsonPath.parseOrNull("$[?@[?@[?@[?@]]]]", shallow).shouldNotBeNull()
            JsonPath.parseOrNull("$[?@[?@[?@[?@[?@]]]]]", shallow).shouldBeNull()
            JsonPath.parseOrNull("$[?length(length(length(@)))]", shallow).shouldBeNull()
            JsonPath.parseOrNull("$[?length(length(length(@))) > 0]", shallow).shouldNotBeNull()
            JsonPath.parseOrNull("$[?length(length(length(length(@)))) > 0]", shallow).shouldBeNull()
        }

        test("never overflows the stack, however deep the expression") {
            JsonPath.parseOrNull("$[?" + "(".repeat(DEEP) + "@" + ")".repeat(DEEP) + "]").shouldBeNull()
            JsonPath.parseOrNull("$" + "[?@".repeat(DEEP) + "]".repeat(DEEP)).shouldBeNull()
            JsonPath.parseOrNull("$[?" + "!(".repeat(DEEP) + "@" + ")".repeat(DEEP) + "]").shouldBeNull()
            JsonPath.parseOrNull("$[?" + "length(".repeat(DEEP) + "@" + ")".repeat(DEEP) + " > 0]").shouldBeNull()
        }

        test("refuses indices that are not canonical and logical function arguments") {
            JsonPath.parseOrNull("$[-]").shouldBeNull()
            JsonPath.parseOrNull("$[01]").shouldBeNull()
            JsonPath.parseOrNull("$[?length(!@.a) == 1]").shouldBeNull()
            JsonPath.parseOrNull("$[?length((@.a)) == 1]").shouldBeNull()
        }

        test("refuses numbers that are not finite or too long") {
            JsonPath.parseOrNull("$[?@ == 1e308]").shouldNotBeNull()
            JsonPath.parseOrNull("$[?@ == 1e999]").shouldBeNull()
            JsonPath.parseOrNull("$[?@ == -1e999]").shouldBeNull()
            JsonPath.parseOrNull("$[?@ == 1e-999]").shouldNotBeNull()
            val short = limits { maximumNumberLength = 5 }
            JsonPath.parseOrNull("$[?@ == 12345]", short).shouldNotBeNull()
            JsonPath.parseOrNull("$[?@ == 123456]", short).shouldBeNull()
        }

        test("refuses a lone surrogate in a name or a string") {
            JsonPath.parseOrNull("$.\uD800").shouldBeNull()
            JsonPath.parseOrNull("$.a\uDC00").shouldBeNull()
            JsonPath.parseOrNull("$['\uD800']").shouldBeNull()
            JsonPath.parseOrNull("$['\uDC00x']").shouldBeNull()
            JsonPath.parseOrNull("$.a\uD800").shouldBeNull()
            JsonPath.parseOrNull("$['\uD800").shouldBeNull()
            JsonPath.parseOrNull("$.😀").shouldNotBeNull()
            JsonPath.parseOrNull("$['😀']").shouldNotBeNull()
        }

        test("compares an integer literal beyond a long exactly") {
            val big = nodeOf("[123456789012345678901234567890, 123456789012345678901234567891]")
            val matches = JsonPath.parse("$[?@ == 123456789012345678901234567890]").query(big)

            matches.map { match -> match.location.text } shouldBe listOf("/0")
        }
    }

    context("evaluation") {
        test("stops at its step budget and never answers in part") {
            val items = yamlSequence((1..ITEMS).map { yamlScalar(it) })
            val needed = minimalSteps(DESCENDANTS, items)

            DESCENDANTS.selectedIn(items, limits { maximumQuerySteps = needed }) shouldHaveSize ITEMS
            val exceeded = shouldThrow<YamlDocumentLimitException> {
                DESCENDANTS.selectedIn(items, limits { maximumQuerySteps = needed - 1 })
            }
            exceeded.limit shouldBe STEPS
        }

        test("stops at its result budget") {
            val three = limits { maximumQueryResults = RESULTS }

            "$[*]".selectedIn(nodeOf("[1, 2, 3]"), three) shouldHaveSize RESULTS
            val exceeded = shouldThrow<YamlDocumentLimitException> { "$[*]".selectedIn(nodeOf("[1, 2, 3, 4]"), three) }
            exceeded.limit shouldBe "maximumQueryResults"
            shouldThrow<YamlDocumentLimitException> { "$..*..*".selectedIn(nodeOf("[[[1, 2]]]"), three) }
        }

        test("walks descendants iteratively, however deep or wide the graph") {
            DESCENDANTS.selectedIn(nested(DEPTH), YamlLimits.Relaxed) shouldHaveSize DEPTH
            val wide = yamlMapping((1..WIDTH).map { YamlEntry(key = yamlScalar("k$it"), value = yamlScalar(it)) })
            val found = "$..k$KEY" selectedIn wide

            found.single().node shouldBe yamlScalar(KEY)
        }

        test("charges comparisons of deep values against the depth limit") {
            val shallow = limits { maximumDepth = LEVELS }
            val deep = yamlSequence(listOf(nested(LEVELS), nested(LEVELS)))
            val deeper = yamlSequence(listOf(nested(LEVELS + 1), nested(LEVELS + 1)))

            SAME_AS_SECOND.selectedIn(deep, shallow) shouldHaveSize 2
            val exceeded = shouldThrow<YamlDocumentLimitException> { SAME_AS_SECOND.selectedIn(deeper, shallow) }
            exceeded.limit shouldBe "maximumDepth"
        }

        test("charges the I-Regexp of match and search against the shared budget") {
            val subjects = yamlSequence(listOf(yamlScalar("a".repeat(SUBJECT_LENGTH) + "!")))
            val pattern = "$[?match(@, '(a*)*b')]"

            (pattern selectedIn subjects).shouldBeEmpty()
            val exceeded = shouldThrow<YamlDocumentLimitException> {
                pattern.selectedIn(subjects, limits { maximumQuerySteps = SMALL_BUDGET })
            }
            exceeded.limit shouldBe STEPS
        }

        test("treats a pattern beyond its limits as no I-Regexp") {
            val short = limits { maximumRegexpLength = RESULTS }
            val subjects = nodeOf("[abc, abcd]")

            "$[?match(@, 'a.c')]".selectedIn(subjects, short) shouldHaveSize 1
            "$[?match(@, 'a..d')]".selectedIn(subjects, short).shouldBeEmpty()
            ("$[?search(@, '(')]" selectedIn subjects).shouldBeEmpty()
        }

        test("charges every operand of a filter") {
            val items = yamlSequence(List(ITEMS) { yamlScalar("x") })
            val few = minimalSteps(conjunction(FEW_OPERANDS), items)
            val many = minimalSteps(conjunction(MANY_OPERANDS), items)

            (many - few >= (MANY_OPERANDS - FEW_OPERANDS).toLong() * ITEMS) shouldBe true
        }

        test("charges every token of the location of a result") {
            val leaf = yamlSequence(List(ITEMS) { yamlScalar(0) })
            val deep = (1..LEVELS_DEEP).fold<Int, YamlNode>(leaf) { node, _ -> yamlSequence(listOf(node)) }

            (minimalSteps(DESCENDANTS, deep) >= ITEMS.toLong() * LEVELS_DEEP) shouldBe true
        }

        test("compares integers with a huge real by magnitude") {
            val integers = yamlSequence(List(INTEGERS) { yamlScalar(it.toLong()) })
            val small = timed { "$[?@ < 1.0]".selectedIn(integers) }
            val huge = timed { "$[?@ < 1e308]".selectedIn(integers) }

            (huge < small * GROWTH + slack) shouldBe true
        }

        test("reads a pattern from the document") {
            val node = nodeOf("[{p: 'a+', s: aaa}, {p: '(', s: a}, {p: 1, s: a}]")

            "$[?match(@.s, @.p)]" selectedIn node shouldHaveSize 1
        }
    }

    context("random input") {
        test("never makes parsing throw") {
            checkAll(expressions) { expression ->
                JsonPath.parseOrNull(expression)
                val failure = runCatching { JsonPath.parse(expression) }.exceptionOrNull()
                (failure == null || failure is IllegalArgumentException) shouldBe true
            }
        }

        test("never makes a query throw anything but a limit") {
            val tight = limits { maximumQuerySteps = TIGHT_BUDGET }
            checkAll(expressions) { expression ->
                val path = JsonPath.parseOrNull("$" + expression, tight)
                val failure = path?.let { runCatching { it.query(document) }.exceptionOrNull() }
                (failure == null || failure is YamlDocumentLimitException) shouldBe true
            }
        }

        test("is shown escaped in every description and message") {
            val hostile = Arb.list(Arb.element(HostileFragments.all), 0..PIECES).map { it.joinToString("") }
            checkAll(hostile) { text ->
                val message = messageOf { JsonPath.parse(text) }
                val raw = text.filter { it >= ' ' && it != '\'' && it != '\\' && it != '\u007F' && !it.isSurrogate() }
                val shown = JsonPath.parse("$['$raw']").toString()

                unsafeCodePointsIn(message).shouldBeEmpty()
                unsafeCodePointsIn(shown).shouldBeEmpty()
            }
        }
    }
})
