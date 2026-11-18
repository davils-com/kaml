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

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.yamlLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val HARD: Int = YamlLimits.HARD_DEPTH
private const val LEAF: String = "leaf-secret"
private const val DOUBLINGS: Int = 64

private fun nested(levels: Int, leaf: YamlNode = yamlScalar(LEAF)): YamlNode {
    var node = leaf
    repeat(levels) { node = yamlSequence(listOf(node)) }
    return node
}

private fun doubled(times: Int, leaf: String): YamlNode {
    var node: YamlNode = yamlScalar(leaf)
    repeat(times) { node = yamlSequence(listOf(node, node)) }
    return node
}

class YamlNodeDepthTest : FunSpec({
    context("the depth of a node") {
        test("counts the collections on the longest path") {
            yamlScalar("a").depth shouldBe 0
            yamlSequence(emptyList()).depth shouldBe 1
            yamlMapping(emptyList()).depth shouldBe 1
            yamlMapping { "a" to "b" }.depth shouldBe 1
            yamlSequence(listOf(yamlScalar("a"), nested(levels = 2))).depth shouldBe 2 + 1
        }

        test("counts the keys and the values of a mapping") {
            yamlMapping(listOf(YamlEntry(key = nested(levels = 2), value = yamlNull()))).depth shouldBe 2 + 1
            yamlMapping(listOf(YamlEntry(key = yamlNull(), value = nested(levels = 1)))).depth shouldBe 2
        }
    }

    context("a node at the hard depth") {
        val deepest = nested(levels = HARD)

        test("is built") {
            deepest.depth shouldBe HARD
        }

        test("compares, hashes and shows itself without overflowing the stack") {
            val twin = nested(levels = HARD)

            deepest shouldBe twin
            deepest.hashCode() shouldBe twin.hashCode()
            deepest shouldNotBe nested(levels = HARD, leaf = yamlScalar("other"))
            deepest.toString() shouldBe "Sequence(<tag:yaml.org,2002:seq>, 1 items)"
        }

        test("is detached without overflowing the stack") {
            val detached = deepest.detached(YamlLimits.Relaxed)

            detached shouldBe deepest
            detached.depth shouldBe HARD
        }

        test("cannot be nested once more in a sequence") {
            val error = shouldThrow<IllegalArgumentException> { yamlSequence(listOf(deepest)) }

            error.message.orEmpty() shouldContain HARD.toString()
            error.message.orEmpty() shouldNotContain LEAF
        }

        test("cannot be nested once more as a key or a value") {
            shouldThrow<IllegalArgumentException> {
                yamlMapping(listOf(YamlEntry(key = deepest, value = yamlNull())))
            }
            shouldThrow<IllegalArgumentException> {
                yamlMapping(listOf(YamlEntry(key = yamlNull(), value = deepest)))
            }
            shouldThrow<IllegalArgumentException> { yamlMapping { "key" to deepest } }
        }

        test("cannot be nested once more by replacing items or entries") {
            val sequence = yamlSequence(emptyList())
            val mapping = yamlMapping(emptyList())

            shouldThrow<IllegalArgumentException> { sequence.withItems(listOf(deepest)) }
            shouldThrow<IllegalArgumentException> {
                mapping.withEntries(listOf(YamlEntry(key = yamlNull(), value = deepest)))
            }
        }
    }

    context("a graph of shared nodes") {
        test("compares equal graphs once per pair of shared nodes") {
            val first = doubled(times = DOUBLINGS, leaf = "x")
            val second = doubled(times = DOUBLINGS, leaf = "x")

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
        }

        test("tells graphs with another leaf apart") {
            doubled(times = DOUBLINGS, leaf = "x") shouldNotBe doubled(times = DOUBLINGS, leaf = "y")
        }

        test("is detached once per shared node") {
            val graph = doubled(times = DOUBLINGS, leaf = "x") as YamlNode.Sequence
            val detached = graph.detached(yamlLimits { maximumNodes = DOUBLINGS + 1L }) as YamlNode.Sequence

            detached shouldBe graph
            detached.items[0] shouldBeSameInstanceAs detached.items[1]
        }

        test("is refused when the copy would exceed the node limit") {
            val graph = doubled(times = DOUBLINGS, leaf = "x")
            val limits = yamlLimits { maximumNodes = DOUBLINGS.toLong() }
            val error = shouldThrow<IllegalArgumentException> { graph.detached(limits) }

            error.message.orEmpty() shouldContain DOUBLINGS.toString()
        }
    }
})
