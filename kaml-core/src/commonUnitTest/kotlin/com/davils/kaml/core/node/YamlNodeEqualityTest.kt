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

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.internal.NodeIdentity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val HASH_FACTOR: Int = 31
private const val FIRST_TEXT: String = "Aa"
private const val COLLIDING_TEXT: String = "BB"
private const val RATIO: Double = 0.5

private fun entryOf(key: YamlNode, value: YamlNode): YamlEntry = YamlEntry(key = key, value = value)

private fun entryHash(key: YamlNode, value: YamlNode): Int = HASH_FACTOR * key.hashCode() + value.hashCode()

private fun shouldCollideButDiffer(first: YamlNode, second: YamlNode) {
    val anything: Any = second

    first.hashCode() shouldBe second.hashCode()
    first shouldNotBe second
    (first == anything) shouldBe false
}

class YamlNodeEqualityTest : FunSpec({
    context("texts with a chosen hash code") {
        test("have that hash code") {
            listOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE, FIRST_TEXT.hashCode()).forEach { hash ->
                textWithHash(hash).hashCode() shouldBe hash
                scalarWithHash(hash).hashCode() shouldBe hash
            }
        }
    }

    context("nodes whose hash codes collide") {
        test("tell scalars apart") {
            FIRST_TEXT.hashCode() shouldBe COLLIDING_TEXT.hashCode()
            shouldCollideButDiffer(first = yamlScalar(FIRST_TEXT), second = yamlScalar(COLLIDING_TEXT))
        }

        test("tell sequences with other items apart") {
            shouldCollideButDiffer(
                first = yamlSequence(listOf(yamlScalar(FIRST_TEXT))),
                second = yamlSequence(listOf(yamlScalar(COLLIDING_TEXT))),
            )
        }

        test("tell sequences of other sizes apart") {
            val single = yamlSequence(listOf(yamlScalar("a")))
            val head = yamlScalar("b")
            val prefix = HASH_FACTOR * YamlTag.SEQUENCE.hashCode() + head.hashCode()
            val pair = yamlSequence(listOf(head, scalarWithHash(single.hashCode() - HASH_FACTOR * prefix)))

            shouldCollideButDiffer(first = single, second = pair)
        }

        test("tell a sequence from a mapping and a scalar") {
            val mapping = yamlMapping { "key" to "value" }
            val itemHash = mapping.hashCode() - HASH_FACTOR * YamlTag.SEQUENCE.hashCode()
            val sequence = yamlSequence(listOf(scalarWithHash(itemHash)))
            val empty = yamlSequence(emptyList())

            shouldCollideButDiffer(first = sequence, second = mapping)
            shouldCollideButDiffer(first = mapping, second = sequence)
            shouldCollideButDiffer(first = empty, second = scalarWithHash(empty.hashCode()))
        }

        test("tell sequences with other tags apart") {
            val local = YamlTag("!list")
            val item = yamlScalar("a")
            val shift = HASH_FACTOR * (local.hashCode() - YamlTag.SEQUENCE.hashCode())

            shouldCollideButDiffer(
                first = yamlSequence(listOf(item), tag = local),
                second = yamlSequence(listOf(scalarWithHash(item.hashCode() + shift))),
            )
        }

        test("tell mappings with colliding keys apart") {
            shouldCollideButDiffer(
                first = yamlMapping { FIRST_TEXT to 1 },
                second = yamlMapping { COLLIDING_TEXT to 1 },
            )
        }

        test("tell mappings with colliding values apart") {
            shouldCollideButDiffer(
                first = yamlMapping { "key" to FIRST_TEXT },
                second = yamlMapping { "key" to COLLIDING_TEXT },
            )
        }

        test("tell mappings without a common key apart") {
            val first = entryOf(key = yamlScalar("a"), value = yamlScalar("x"))
            val otherKey = yamlScalar("b")
            val firstHash = entryHash(key = first.key, value = first.value)
            val value = scalarWithHash(firstHash - HASH_FACTOR * otherKey.hashCode())

            val other = entryOf(key = otherKey, value = value)

            shouldCollideButDiffer(first = yamlMapping(listOf(first)), second = yamlMapping(listOf(other)))
        }

        test("tell mappings of other sizes apart") {
            val single = entryOf(key = yamlScalar("a"), value = yamlScalar("x"))
            val head = entryOf(key = yamlScalar("b"), value = yamlScalar("y"))
            val tailKey = yamlScalar("c")
            val rest = entryHash(key = single.key, value = single.value) - entryHash(key = head.key, value = head.value)
            val tail = entryOf(key = tailKey, value = scalarWithHash(rest - HASH_FACTOR * tailKey.hashCode()))

            shouldCollideButDiffer(first = yamlMapping(listOf(single)), second = yamlMapping(listOf(head, tail)))
        }
    }

    context("finding a value by a node key") {
        val mapping = yamlMapping {
            FIRST_TEXT to 1
            "b" to 2
        }

        test("finds nothing for a missing key") {
            mapping[yamlScalar("missing")] shouldBe null
        }

        test("finds nothing for a key whose hash code collides") {
            mapping[yamlScalar(COLLIDING_TEXT)] shouldBe null
            mapping[yamlScalar(FIRST_TEXT)].int() shouldBe 1
        }
    }

    context("collections compared with other values") {
        test("are never equal to them") {
            val text: Any = "a"

            (yamlSequence { add("a") } == text) shouldBe false
            (yamlMapping { "a" to 1 } == text) shouldBe false
        }
    }

    context("identities of nodes") {
        val node = yamlScalar("a")
        val other = yamlScalar("a")

        test("compare by instance") {
            val anything: Any = NodeIdentity(node)

            (NodeIdentity(node) == NodeIdentity(node)) shouldBe true
            (NodeIdentity(node) == NodeIdentity(other)) shouldBe false
            anything shouldNotBe node
        }

        test("compare pairs by both instances") {
            val anything: Any = NodePair(first = node, second = other)

            val pair = NodePair(first = node, second = other)

            (pair == NodePair(first = node, second = other)) shouldBe true
            pair.hashCode() shouldBe NodePair(first = node, second = other).hashCode()
            (pair == NodePair(first = other, second = other)) shouldBe false
            (pair == NodePair(first = node, second = node)) shouldBe false
            anything shouldNotBe node
        }
    }

    context("the builder of a mapping") {
        test("takes values of every type for a node key") {
            val built = yamlMapping {
                yamlScalar("text") to "a"
                yamlScalar("long") to 1L
                yamlScalar("int") to 2
                yamlScalar("double") to RATIO
                yamlScalar("boolean") to true
            }

            built["text"].string() shouldBe "a"
            built["long"].long() shouldBe 1L
            built["int"].int() shouldBe 2
            built["double"].double() shouldBe RATIO
            built["boolean"].boolean() shouldBe true
        }
    }
})
