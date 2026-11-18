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

package com.davils.kaml.serialization.json

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.node.YamlNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

private fun root(text: String): YamlNode {
    val parsed = Yaml.Default.parse(text) as YamlParseResult.Parsed
    return parsed.stream.documents
        .single()
        .root
}

class YamlJsonTest : FunSpec({
    context("converting YAML to JSON") {
        test("converts every kind of value") {
            val converted = root("{a: 1, b: 1.5, c: true, d: ~, e: text, f: [1, x], g: 0x10}").toJsonElement()
            val expected = Json.parseToJsonElement(
                """{"a":1,"b":1.5,"c":true,"d":null,"e":"text","f":[1,"x"],"g":16}""",
            )

            converted.shouldBeInstanceOf<YamlJsonConversion.Converted>().element shouldBe expected
        }

        test("refuses a key that is no scalar") {
            val refused = root("{[a]: b}").toJsonElement()

            refused.shouldBeInstanceOf<YamlJsonConversion.Refused>().reason shouldBe "JSON keys must be strings"
        }

        test("refuses a number that JSON cannot hold") {
            val infinity = root(".inf").toJsonElement()
            val overflow = root("1e400").toJsonElement()

            infinity.shouldBeInstanceOf<YamlJsonConversion.Refused>()
            overflow.shouldBeInstanceOf<YamlJsonConversion.Refused>()
        }

        test("keeps a tagged scalar as a string") {
            val conversion = root("!x 12").toJsonElement()
            val converted = conversion.shouldBeInstanceOf<YamlJsonConversion.Converted>()

            converted.element shouldBe JsonPrimitive("12")
        }
    }

    context("converting JSON to YAML") {
        test("converts every kind of value") {
            val json = Json.parseToJsonElement("""{"a":1,"b":1.5,"c":true,"d":null,"e":"text","f":[1,"x"]}""")

            json.toYamlNode() shouldBe root("{a: 1, b: 1.5, c: true, d: null, e: text, f: [1, x]}")
        }

        test("keeps a literal that is no number as a scalar") {
            JsonNull.toYamlNode() shouldBe root("null")
            JsonPrimitive(false).toYamlNode() shouldBe root("false")
        }
    }
})
