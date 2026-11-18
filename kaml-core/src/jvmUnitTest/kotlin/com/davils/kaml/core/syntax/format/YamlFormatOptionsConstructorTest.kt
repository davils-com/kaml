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

package com.davils.kaml.core.syntax.format

import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.lang.reflect.InvocationTargetException

private const val WIDE_INDENT: Int = 100_000
private const val INDENT: Int = 4

private fun optionsOf(configure: YamlFormatOptionsBuilder.() -> Unit): YamlFormatOptions {
    val builder = YamlFormatOptionsBuilder::class.java.getConstructor().newInstance()
    builder.configure()
    val constructor = YamlFormatOptions::class.java.getConstructor(YamlFormatOptionsBuilder::class.java)
    return constructor.newInstance(builder)
}

class YamlFormatOptionsConstructorTest : FunSpec({
    context("the constructor a Java caller can reach") {
        test("refuses an indent or a number of blank lines out of range") {
            listOf<YamlFormatOptionsBuilder.() -> Unit>(
                { indent = WIDE_INDENT },
                { indent = 0 },
                { maximumEmptyLines = -1 },
            ).forEach { configure ->
                val thrown = shouldThrow<InvocationTargetException> { optionsOf(configure) }
                thrown.cause.shouldBeInstanceOf<ValidationException>()
            }
        }

        test("builds options within range") {
            optionsOf { indent = INDENT } shouldBe yamlFormatOptions { indent = INDENT }
        }
    }
})
