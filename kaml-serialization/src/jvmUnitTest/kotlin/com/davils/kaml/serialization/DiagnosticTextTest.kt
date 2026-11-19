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
package com.davils.kaml.serialization

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kore.core.Limits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.builtins.serializer

private val beyondLineMap: String = "#" + "a".repeat(Limits.MAX_LINE_MAP_LENGTH)

class DiagnosticTextTest : FunSpec({
    context("a text longer than a line map indexes") {
        test("is refused with a decoding exception that names its diagnostic") {
            val failure = shouldThrow<YamlDecodingException> {
                YamlFormat.Default.decodeFromString(Int.serializer(), beyondLineMap)
            }
            failure.diagnostics.map { it.code } shouldContainExactly listOf(YamlDiagnosticCode.InputTooLong)
        }
    }
})
