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

package com.davils.kaml.document.redact

import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

private const val INVALID_PATH: String = "$..password["
private const val INJECTED_REPLACEMENT: String = "***\nadmin: true"

private fun unbuiltBuilder(): YamlRedactionBuilder {
    val constructor = YamlRedactionBuilder::class.java.getConstructor()
    val builder = constructor.newInstance()
    builder.path(INVALID_PATH)
    builder.replacement = INJECTED_REPLACEMENT
    return builder
}

class YamlRedactionJavaTest : FunSpec({
    context("the constructor that Java reaches") {
        test("is public in the bytecode") {
            val constructor = YamlRedaction::class.java.getConstructor(YamlRedactionBuilder::class.java)

            Modifier.isPublic(constructor.modifiers) shouldBe true
        }

        test("validates the builder instead of trusting the DSL to have done it") {
            val constructor = YamlRedaction::class.java.getConstructor(YamlRedactionBuilder::class.java)
            val builder = unbuiltBuilder()

            val failure = shouldThrow<InvocationTargetException> { constructor.newInstance(builder) }
            failure.cause.shouldBeInstanceOf<ValidationException>()
        }
    }
})
