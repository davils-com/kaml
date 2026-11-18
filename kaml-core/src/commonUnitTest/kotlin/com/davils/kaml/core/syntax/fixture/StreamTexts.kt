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

package com.davils.kaml.core.syntax.fixture

import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map

private const val MOST_PIECES: Int = 14

internal val streamPieces: List<String> = listOf(
    "---\n", "...\n", "--- ", "--- |\n", "--- >\n", "--- a\n", "%YAML 1.2\n", "%YAML 1.1\n", "%TAG !e! tag:x,1:\n",
    "a: 1\n", "b:\n", "  c: 2\n", "- x\n", "  - y\n", "\"q\n", "'q\n", "\"\n", "'\n", "|\n", ">-\n", "  t\n",
    "[a,\n", "]\n", "{k: \n", "}\n", "# c\n", "\n", "\uFEFF", "? k\n", ": v\n", "&a x\n", "*a\n", "!e!t z\n",
    "\t", " ", "\r\n", "\r", "x", ":", "-", "#", "...x\n", "---x\n", "%", "@",
)

internal val streamTexts: Arb<String> = Arb.list(Arb.element(streamPieces), 1..MOST_PIECES).map { pieces ->
    pieces.joinToString(separator = "")
}
