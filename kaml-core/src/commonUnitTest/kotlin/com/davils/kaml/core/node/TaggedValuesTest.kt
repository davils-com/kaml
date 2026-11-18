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
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlTag
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain

private const val SIXTEEN: Long = 16L
private const val HIDDEN_VALUE: String = "not-a-number"

class TaggedValuesTest : FunSpec({
    context("a scalar tagged with a core tag") {
        test("equals the scalar its value is read as") {
            yamlTagged("1", YamlTag.INTEGER) shouldBe yamlScalar(1L)
            yamlTagged("0x10", YamlTag.INTEGER) shouldBe yamlScalar(SIXTEEN)
            yamlTagged("1.0", YamlTag.FLOAT) shouldBe yamlScalar(1.0)
            yamlTagged("True", YamlTag.BOOLEAN) shouldBe Yaml.Default.root("true")
            yamlTagged("~", YamlTag.NULL) shouldBe yamlNull()
            yamlTagged("a", YamlTag.STRING) shouldBe yamlScalar("a")
        }

        test("reads back equal after it was emitted") {
            val mapping = yamlMapping {
                yamlTagged("0x10", YamlTag.INTEGER) to "tagged"
                yamlTagged("true", YamlTag.BOOLEAN) to "flag"
            }
            Yaml.Default.root(Yaml.Default.emit(mapping)) shouldBe mapping
        }

        test("is the same key as the scalar its value is read as") {
            val entries = listOf(
                YamlEntry(key = yamlTagged("1", YamlTag.INTEGER), value = yamlScalar("tagged")),
                YamlEntry(key = yamlScalar(1L), value = yamlScalar("built")),
            )
            shouldThrow<IllegalArgumentException> { yamlMapping(entries) }
        }

        test("is refused when its value is no value of the tag, without showing the value") {
            listOf(YamlTag.INTEGER, YamlTag.FLOAT, YamlTag.BOOLEAN, YamlTag.NULL).forEach { tag ->
                val failure = shouldThrow<IllegalArgumentException> { yamlTagged(HIDDEN_VALUE, tag) }
                failure.message.orEmpty() shouldNotContain HIDDEN_VALUE
            }
        }

        test("is compared by its text when it is a number too long to be converted") {
            val long = "1".repeat(YamlLimits.HARD_NUMBER_LENGTH + 1)
            yamlTagged(long, YamlTag.INTEGER) shouldNotBe yamlTagged("0$long", YamlTag.INTEGER)
            val longest = long.drop(2)
            yamlTagged(longest, YamlTag.INTEGER) shouldBe yamlTagged("0$longest", YamlTag.INTEGER)
        }
    }

    context("a scalar tagged with another tag") {
        test("is compared by its text") {
            yamlTagged("1", YamlTag("!x")) shouldNotBe yamlScalar(1L)
            yamlTagged("x", YamlTag("!x")) shouldBe yamlTagged("x", YamlTag("!x"))
        }
    }
})
