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
package com.davils.kaml.core.schema

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.node.double
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

private val overflows: List<Pair<String, Yaml>> = listOf(
    "a: 1e400" to Yaml.Default,
    "a: -1e400" to Yaml.Default,
    "a: !!float 1e400" to Yaml.Default,
    "a: 1.0e+400" to yaml { schema = YamlSchema.Yaml11 },
    "a: 1e400" to yaml { schema = YamlSchema.Json },
)

class FloatOverflowTest : FunSpec({
    context("a float too large for a double") {
        overflows.forEach { (document, reader) ->
            test("is an invalid tagged value in $document read with ${reader.configuration.schema.name}") {
                reader.problems(document) shouldContain YamlDiagnosticCode.InvalidTaggedValue
            }
        }
    }

    context("an infinity") {
        test("is read from .inf and -.inf") {
            val root = Yaml.Default.root("a: .inf\nb: -.inf")
            root["a"] shouldBe yamlScalar(Double.POSITIVE_INFINITY)
            root["b"] shouldBe yamlScalar(Double.NEGATIVE_INFINITY)
        }
    }

    context("a float too small for a double") {
        test("is read as zero") {
            Yaml.Default.root("a: 1e-400")["a"].double() shouldBe 0.0
        }
    }
})
