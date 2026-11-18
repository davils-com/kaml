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
import com.davils.kaml.core.model.YamlCollectionStyle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val HTTP: Long = 80L

class YamlReplacingTest : FunSpec({
    context("a mapping with other entries") {
        val mapping = Yaml.Default.root(text("&server {name: web}")).shouldBeInstanceOf<YamlNode.Mapping>()
        val entries = listOf(YamlEntry(key = yamlScalar("port"), value = yamlScalar(HTTP)))

        test("holds the new entries") {
            mapping.withEntries(entries)["port"].long() shouldBe HTTP
            mapping.withEntries(entries)["name"] shouldBe null
        }

        test("keeps the place, the style, the tag and the anchor of the mapping") {
            val replaced = mapping.withEntries(entries)

            replaced.range.shouldNotBeNull() shouldBe mapping.range
            replaced.style shouldBe YamlCollectionStyle.Flow
            replaced.tag shouldBe mapping.tag
            replaced.anchor shouldBe mapping.anchor
        }

        test("refuses entries with a key given twice") {
            shouldThrow<IllegalArgumentException> { mapping.withEntries(entries + entries) }
        }
    }

    context("a sequence with other items") {
        val sequence = Yaml.Default.root(text("[80, 443]")).shouldBeInstanceOf<YamlNode.Sequence>()

        test("holds the new items and keeps the place and the style of the sequence") {
            val replaced = sequence.withItems(listOf(yamlScalar(HTTP)))

            replaced.items.single().long() shouldBe HTTP
            replaced.range.shouldNotBeNull() shouldBe sequence.range
            replaced.style shouldBe YamlCollectionStyle.Flow
            replaced.tag shouldBe sequence.tag
            replaced.anchor shouldBe sequence.anchor
        }
    }
})
