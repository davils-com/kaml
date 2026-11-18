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

package com.davils.kaml.core.node

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.fixture.collidingText
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.timeRatio
import com.davils.kaml.core.model.YamlTag
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val FEW: Int = 1_000
private const val MANY: Int = 8_000
private const val MOST_GROWTH: Double = 24.0
private const val PREFIX: String = "kkkkkkkkkkkkkkkk"
private const val SIXTEEN: Long = 16L

private fun collidingKey(index: Int): YamlNode = yamlScalar(PREFIX + collidingText(index))

private fun collidingEntries(count: Int): List<YamlEntry> =
    (0 until count).map { index -> YamlEntry(key = collidingKey(index), value = yamlScalar(index.toLong())) }

private fun collidingMapping(count: Int): YamlNode.Mapping = yamlMapping(collidingEntries(count))

private fun build(count: Int) {
    collidingMapping(count)
}

private fun compareTwice(count: Int) {
    (collidingMapping(count) == collidingMapping(count)) shouldBe true
}

private fun shouldOrderAsEqual(first: YamlNode, second: YamlNode) {
    NodeOrder.compare(first = first, second = second) shouldBe 0
    NodeOrder.compare(first = second, second = first) shouldBe 0
}

private fun shouldOrderApart(first: YamlNode, second: YamlNode) {
    val forward = NodeOrder.compare(first = first, second = second)
    val backward = NodeOrder.compare(first = second, second = first)
    forward shouldNotBe 0
    forward * backward shouldBeLessThan 0
}

class NodeOrderTest : FunSpec({
    context("keys whose hash codes collide") {
        test("share one hash code and differ") {
            val keys = (0 until FEW).map(::collidingKey)
            keys.map(YamlNode::hashCode).toSet().size shouldBe 1
            keys.toSet().size shouldBe FEW
        }

        test("build a mapping in time that grows with their number, not its square") {
            val ratio = timeRatio(small = "$FEW", large = "$MANY") { count -> build(count.toInt()) }
            ratio shouldBeLessThan MOST_GROWTH
        }

        test("compare two mappings in time that grows with their number, not its square") {
            val ratio = timeRatio(small = "$FEW", large = "$MANY") { count -> compareTwice(count.toInt()) }
            ratio shouldBeLessThan MOST_GROWTH
        }

        test("are each found in their mapping") {
            val mapping = collidingMapping(FEW)
            for (index in 0 until FEW) {
                mapping[collidingKey(index)] shouldBe yamlScalar(index.toLong())
            }
            mapping[collidingKey(FEW)] shouldBe null
        }

        test("are refused when one repeats, by the index of the repeat") {
            val entries = collidingEntries(FEW) + YamlEntry(key = collidingKey(1), value = yamlNull())
            val failure = shouldThrow<IllegalArgumentException> { yamlMapping(entries) }
            failure.message.orEmpty() shouldContain "entry $FEW repeats"
        }

        test("are added to a key set once") {
            val keys = NodeKeySet()
            for (index in 0 until FEW) {
                keys.add(collidingKey(index)) shouldBe true
            }
            for (index in 0 until FEW) {
                keys.add(collidingKey(index)) shouldBe false
            }
        }
    }

    context("compare") {
        test("orders equal values of different texts as equal") {
            shouldOrderAsEqual(first = Yaml.Default.root("0x10"), second = yamlScalar(SIXTEEN))
            shouldOrderAsEqual(first = Yaml.Default.root("1.0"), second = Yaml.Default.root("1.00"))
            val reordered = Yaml.Default.root("{b: [2], a: 1}")
            shouldOrderAsEqual(first = Yaml.Default.root("{a: 1, b: [2]}"), second = reordered)
            shouldOrderAsEqual(first = Yaml.Default.root("[~, true]"), second = Yaml.Default.root("[null, True]"))
        }

        test("orders nodes of another kind, tag or value apart") {
            shouldOrderApart(first = yamlScalar("1"), second = yamlScalar(1L))
            shouldOrderApart(first = yamlScalar(1.0), second = yamlScalar(-1.0))
            shouldOrderApart(first = yamlScalar(true), second = yamlScalar(false))
            shouldOrderApart(first = yamlTagged("a", YamlTag("!x")), second = yamlScalar("a"))
            val longer = yamlSequence(listOf(yamlNull(), yamlNull()))
            shouldOrderApart(first = yamlSequence(listOf(yamlNull())), second = longer)
            shouldOrderApart(first = Yaml.Default.root("{a: 1}"), second = Yaml.Default.root("{a: 2}"))
            shouldOrderApart(first = Yaml.Default.root("{a: 1}"), second = Yaml.Default.root("[a, 1]"))
        }

        test("orders collections whose items collide by their items") {
            val first = yamlSequence(listOf(collidingKey(0), collidingKey(1)))
            val second = yamlSequence(listOf(collidingKey(1), collidingKey(0)))
            first.hashCode() shouldBe second.hashCode()
            shouldOrderApart(first = first, second = second)
            shouldOrderAsEqual(first = first, second = yamlSequence(listOf(collidingKey(0), collidingKey(1))))
        }

        test("agrees with equality on shared nodes") {
            val shared = Yaml.Default.root("[a, [b, {c: d}]]")
            val twice = yamlSequence(listOf(shared, shared))
            val copy = yamlSequence(listOf(shared, Yaml.Default.root("[a, [b, {c: d}]]")))
            shouldOrderAsEqual(first = twice, second = copy)
            NodeOrder.compare(twice, twice) shouldBe 0
        }
    }
})
