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

import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.internal.NodeFactory
import com.davils.kaml.core.node.internal.ScalarValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun scalarOf(value: String, tag: YamlTag, canonical: ScalarValue): YamlNode.Scalar =
    NodeFactory.scalar(value = value, tag = tag, style = YamlScalarStyle.Plain, canonical = canonical)

class ScalarConstructionTest : FunSpec({
    context("a scalar created through its constructor") {
        test("is refused with a value of another tag") {
            shouldThrow<IllegalArgumentException> { scalarOf("1", YamlTag.STRING, ScalarValue.Integer("1")) }
            shouldThrow<IllegalArgumentException> { scalarOf("1", YamlTag.FLOAT, ScalarValue.Integer("1")) }
            shouldThrow<IllegalArgumentException> { scalarOf("true", YamlTag.INTEGER, ScalarValue.Truth(true)) }
            shouldThrow<IllegalArgumentException> { scalarOf("x", YamlTag("!x"), ScalarValue.Null) }
        }

        test("is refused as a string whose value is another text") {
            shouldThrow<IllegalArgumentException> { scalarOf("a", YamlTag.STRING, ScalarValue.Text("b")) }
        }

        test("keeps a text for a tag whose value could not be read") {
            scalarOf("0x", YamlTag.INTEGER, ScalarValue.Text("0x")).value shouldBe "0x"
            scalarOf("1.0", YamlTag.FLOAT, ScalarValue.Real(1.0)).value shouldBe "1.0"
        }
    }
})
