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

package com.davils.kaml.core.compose

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.fixture.collidingWord
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.stream
import com.davils.kaml.core.fixture.timeRatio
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.edit.YamlEditResult
import com.davils.kaml.core.syntax.edit.YamlEditor
import com.davils.kaml.core.syntax.edit.YamlPath
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val FEW_BITS: Int = 10
private const val MANY_BITS: Int = 13
private const val MOST_GROWTH: Double = 24.0
private const val LINE_BREAK: String = "\n"
private const val FEW: Int = 1 shl FEW_BITS

private fun words(bits: Int): List<String> = List(1 shl bits) { index -> collidingWord(bits, index) }

private fun anchored(bits: Int): String {
    val names = words(bits)
    val anchors = names.mapIndexed { index, name -> "a$index: &$name v$index" }
    val aliases = names.mapIndexed { index, name -> "b$index: *$name" }
    return (anchors + aliases).joinToString(LINE_BREAK)
}

private fun tagged(bits: Int): String {
    val directives = words(bits).map { name -> "%TAG !$name! tag:example.com,2026:" }
    return (directives + "--- x").joinToString(LINE_BREAK)
}

private fun parse(text: String) {
    Yaml.Default.parse(text)
}

private fun reemit(text: String) {
    Yaml.Default.emitEvents(Yaml.Default.events(YamlSource.of(text)).events)
}

private fun edited(text: String): YamlEditResult {
    val editor = YamlEditor(YamlSyntaxTree.parse(text))
    return editor.set(YamlPath.ROOT.key("b0"), yamlScalar("w"))
}

private fun edit(text: String) {
    edited(text)
}

class CollidingAnchorsTest : FunSpec({
    context("anchor names whose hash codes collide") {
        test("are resolved to their own anchors") {
            val root = Yaml.Default.root(anchored(FEW_BITS))

            root["b5"].string() shouldBe "v5"
            root["b${FEW - 1}"].string() shouldBe "v${FEW - 1}"
        }

        test("are composed in time that grows with their number, not its square") {
            timeRatio(small = anchored(FEW_BITS), large = anchored(MANY_BITS), read = ::parse) shouldBeLessThan
                MOST_GROWTH
        }

        test("are emitted again in time that grows with their number, not its square") {
            timeRatio(small = anchored(FEW_BITS), large = anchored(MANY_BITS), read = ::reemit) shouldBeLessThan
                MOST_GROWTH
        }

        test("are edited around in time that grows with their number, not its square") {
            edited(anchored(FEW_BITS)).shouldBeInstanceOf<YamlEditResult.Edited>()
            timeRatio(small = anchored(FEW_BITS), large = anchored(MANY_BITS), read = ::edit) shouldBeLessThan
                MOST_GROWTH
        }
    }

    context("tag handles whose hash codes collide") {
        test("are all kept by the document") {
            val stream = Yaml.Default.stream(tagged(FEW_BITS))
            val document = stream.documents.single()

            document.tagDirectives.size shouldBe FEW
        }

        test("are read and written in time that grows with their number, not its square") {
            timeRatio(small = tagged(FEW_BITS), large = tagged(MANY_BITS), read = ::reemit) shouldBeLessThan MOST_GROWTH
        }
    }
})
