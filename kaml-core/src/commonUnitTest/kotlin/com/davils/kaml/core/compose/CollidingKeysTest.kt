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

package com.davils.kaml.core.compose

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.collidingText
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.timeRatio
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val FEW: Int = 1_000
private const val MANY: Int = 8_000
private const val MOST_GROWTH: Double = 24.0
private const val PREFIX: String = "kkkkkkkkkkkkkkkk"
private const val LINE_BREAK: String = "\n"

private val legacy: Yaml = yaml { schema = YamlSchema.Yaml11 }

private fun key(index: Int): String = PREFIX + collidingText(index)

private fun collidingMapping(count: Int): String =
    (0 until count).joinToString("") { index -> "${key(index)}: $index" + LINE_BREAK }

private fun mergedMapping(count: Int): String {
    val base = (0 until count).joinToString(", ") { index -> "${key(index)}: $index" }
    return listOf("base: &b {$base}", "item:", "  <<: *b", "  ${key(0)}: own").joinToString(LINE_BREAK)
}

private fun parse(text: String) {
    Yaml.Default.parse(text)
}

private fun parseLegacy(text: String) {
    legacy.parse(text)
}

class CollidingKeysTest : FunSpec({
    context("mapping keys whose hash codes collide") {
        test("are composed in time that grows with their number, not its square") {
            val ratio = timeRatio(small = collidingMapping(FEW), large = collidingMapping(MANY), read = ::parse)
            ratio shouldBeLessThan MOST_GROWTH
        }

        test("are merged in time that grows with their number, not its square") {
            val ratio = timeRatio(small = mergedMapping(FEW), large = mergedMapping(MANY), read = ::parseLegacy)
            ratio shouldBeLessThan MOST_GROWTH
        }

        test("are merged without overriding an explicit key") {
            val item = legacy.root(mergedMapping(FEW))["item"].shouldBeInstanceOf<YamlNode.Mapping>()
            item.entries.size shouldBe FEW
            item[key(0)].string() shouldBe "own"
            item[key(FEW - 1)].string() shouldBe "${FEW - 1}"
        }

        test("are reported when one repeats") {
            val text = collidingMapping(FEW) + "${key(1)}: again"
            Yaml.Default.problems(text) shouldContainExactly listOf(YamlDiagnosticCode.DuplicateKey)
        }

        test("keep the first or the last of a repeated key as configured") {
            val text = collidingMapping(FEW) + "${key(1)}: again"
            val first = yaml { duplicateKeys = YamlDuplicateKeys.KeepFirst }.root(text)
            val last = yaml { duplicateKeys = YamlDuplicateKeys.KeepLast }.root(text)
            first[key(1)].string() shouldBe "1"
            last[key(1)].string() shouldBe "again"
            val entries = last.shouldBeInstanceOf<YamlNode.Mapping>().entries
            entries.last().value.string() shouldBe "again"
        }
    }
})
