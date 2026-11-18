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

package com.davils.kaml.core.parser

import com.davils.kaml.core.fixture.timeRatio
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThan

private const val FEW: Int = 4_000
private const val MANY: Int = 32_000
private const val MOST_LINEAR_GROWTH: Double = 24.0
private const val SHALLOW: Int = 10
private const val DEEP: Int = 80
private const val COMMENT_LINES: Int = 16_000
private const val MOST_CONSTANT_GROWTH: Double = 3.0

private fun read(text: String) {
    SyntaxParser.parse(text)
}

private fun flowSequence(items: Int): String = "[" + "a,".repeat(items) + "a]"

private fun flowPairs(items: Int): String = "[" + "a: b, ".repeat(items) + "a]"

private fun directive(parameters: Int): String = "%FOO" + " a".repeat(parameters) + "\n--- x\n"

private fun nestedWithComments(depth: Int): String {
    val levels = List(depth) { level -> " ".repeat(level) + "k:" }
    val comments = ("# " + "x".repeat(78) + "\n").repeat(COMMENT_LINES)
    return levels.joinToString("\n") + " v\n" + comments + "z: 1\n"
}

class LinearParsingTest : FunSpec({
    context("a long line") {
        test("of a flow sequence is parsed in time linear in its length") {
            val ratio = timeRatio(small = flowSequence(FEW), large = flowSequence(MANY), read = ::read)
            ratio shouldBeLessThan MOST_LINEAR_GROWTH
        }

        test("of flow pairs is parsed in time linear in its length") {
            val ratio = timeRatio(small = flowPairs(FEW), large = flowPairs(MANY), read = ::read)
            ratio shouldBeLessThan MOST_LINEAR_GROWTH
        }

        test("of directive parameters is parsed in time linear in its length") {
            val ratio = timeRatio(small = directive(FEW), large = directive(MANY), read = ::read)
            ratio shouldBeLessThan MOST_LINEAR_GROWTH
        }
    }

    context("comment lines after nested blocks") {
        test("are searched once however many blocks they end") {
            val shallow = nestedWithComments(SHALLOW)
            val ratio = timeRatio(small = shallow, large = nestedWithComments(DEEP), read = ::read)
            ratio shouldBeLessThan MOST_CONSTANT_GROWTH
        }
    }
})
