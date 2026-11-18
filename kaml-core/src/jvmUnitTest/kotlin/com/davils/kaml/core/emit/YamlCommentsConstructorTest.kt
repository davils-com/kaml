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

package com.davils.kaml.core.emit

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.internal.NodeIdentity
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

private val key: YamlNode = yamlScalar("name")

private fun built(lines: List<String>): YamlComments {
    val constructor = YamlComments::class.java.getConstructor(Map::class.java)
    Modifier.isPublic(constructor.modifiers) shouldBe true
    return constructor.newInstance(mapOf(NodeIdentity(key) to lines))
}

private fun refusedCause(lines: List<String>): Throwable {
    val thrown = shouldThrow<InvocationTargetException> { built(lines) }
    return thrown.cause.shouldBeInstanceOf<IllegalArgumentException>()
}

class YamlCommentsConstructorTest : FunSpec({
    context("the constructor a Java caller can reach") {
        test("refuses a line that holds a line break or a character that could break the YAML") {
            listOf("note\nadmin: true", "note\radmin: true", "a\u2028b", "a\u202Eb").forEach { line ->
                refusedCause(listOf(line))
            }
        }

        test("keeps a copy of the lines it was given") {
            val lines = mutableListOf("note")
            val comments = built(lines)
            lines += "admin: true"
            val node = yamlMapping(entries = listOf(YamlEntry(key = key, value = yamlScalar("x"))))

            comments.of(key) shouldBe listOf("note")
            Yaml.Default.emit(YamlDocument(node), comments) shouldBe "# note\nname: x\n"
        }
    }
})
