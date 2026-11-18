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
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

private const val HTTPS: Long = 443L

private const val RATIO: Double = 0.5

private const val FIRST: String = "first"

private fun YamlNode.ranges(): List<Any?> = when (this) {
    is YamlNode.Scalar -> listOf(range)
    is YamlNode.Sequence -> listOf(range) + items.flatMap { item -> item.ranges() }
    is YamlNode.Mapping -> listOf(range) + entries.flatMap { entry -> entry.key.ranges() + entry.value.ranges() }
}

class YamlDetachingTest : FunSpec({
    context("a detached node") {
        val node = Yaml.Default.root(text("name: &web 'web'", "ports: [80, 443]", "nested: {on: true, ratio: 0.5}"))

        test("equals the node it was detached from") {
            node.range.shouldNotBeNull()

            node.detached() shouldBe node
        }

        test("names no place in a source at any depth") {
            val detached = node.detached()

            detached.ranges().forEach { range -> range.shouldBeNull() }
        }

        test("keeps the tag, the style, the anchor and the value of every scalar") {
            val detached = node.detached()
            val name = detached["name"].shouldBeInstanceOf<YamlNode.Scalar>()
            val original = node["name"].shouldBeInstanceOf<YamlNode.Scalar>()

            name.tag shouldBe YamlTag.STRING
            name.style shouldBe YamlScalarStyle.SingleQuoted
            name.anchor shouldBe original.anchor
            detached["ports"][1].long() shouldBe HTTPS
            detached["nested"]["ratio"].double() shouldBe RATIO
        }
    }

    context("a detached graph with aliases") {
        val node = Yaml.Default.root(text("$FIRST: &shared [80, 443]", "second: *shared"))

        test("keeps the shared node shared") {
            val detached = node.detached()

            detached[FIRST] shouldBeSameInstanceAs detached["second"]
            detached[FIRST] shouldNotBeSameInstanceAs node[FIRST]
            detached shouldBe node
        }
    }
})
