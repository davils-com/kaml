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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.core.schema

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.hostileText
import com.davils.kaml.core.fixture.shouldBeSafeToShow
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.yamlTagged
import com.davils.kaml.core.yaml
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val ITERATIONS: Int = 200
private const val SECRET: String = "secret-value"
private const val PORT: String = "!port"

private val checking: Yaml = yaml {
    schema = yamlSchema {
        scalar(PORT) { accepts { text -> text.toInt() > 0 } }
    }
}

private val matching: Yaml = yaml {
    schema = yamlSchema {
        scalar(PORT) { implicit { text -> text.toInt() > 0 } }
    }
}

private val failing: Yaml = yaml {
    schema = yamlSchema {
        scalar("!broken") { accepts { _ -> throw NotImplementedError() } }
    }
}

private fun portDocument(value: String): String =
    Yaml.Default.emit(yamlTagged(value, YamlTag(PORT), YamlScalarStyle.DoubleQuoted))

private fun Yaml.shouldFailRedacted(text: String, hidden: String) {
    val result = parse(text)
    result.shouldBeInstanceOf<YamlParseResult.Failed>()
    result.diagnostics.map { it.code } shouldContainExactly listOf(YamlDiagnosticCode.InvalidTaggedValue)
    val message = result.diagnostics.single().message
    message shouldContain SafeText.REDACTED
    message shouldNotContain hidden
    message shouldNotContain "NumberFormatException"
    message.shouldBeSafeToShow()
}

class SchemaCallbackTest : FunSpec({
    context("a check of a custom tag that throws") {
        test("fails the document with a redacted diagnostic") {
            checking.shouldFailRedacted(text = "$PORT $SECRET\n", hidden = SECRET)
        }

        test("never shows a hostile value") {
            checkAll(ITERATIONS, hostileText) { value ->
                val representable = value.filterNot(Char::isSurrogate)
                checking.shouldFailRedacted(text = portDocument(representable + SECRET), hidden = SECRET)
            }
        }
    }

    context("an implicit matcher of a custom tag that throws") {
        test("fails the document with a redacted diagnostic") {
            matching.shouldFailRedacted(text = "- $SECRET\n", hidden = SECRET)
        }
    }

    context("an error of a custom tag") {
        test("is not caught") {
            shouldThrow<NotImplementedError> { failing.parse("!broken value\n") }
        }
    }
})
