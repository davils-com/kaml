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

package com.davils.kaml.core.fixture

private const val MIDDLE: Int = 0x7000
private const val FIRST_SHIFT: Int = -396
private const val LAST_SHIFT: Int = 280
private const val HASH_FACTOR: Int = 31
private const val DISTINCT_BASE: Int = 0x5000
private const val DISTINCT_WIDTH: Int = 1_000

private val blocks: List<String> = (FIRST_SHIFT..LAST_SHIFT).map { shift ->
    "${(MIDDLE + shift).toChar()}${(MIDDLE - HASH_FACTOR * shift).toChar()}"
}

internal fun collidingText(index: Int): String = blocks[index / blocks.size] + blocks[index % blocks.size]

internal fun distinctText(index: Int): String =
    "${(DISTINCT_BASE + index / DISTINCT_WIDTH).toChar()}${(DISTINCT_BASE + index % DISTINCT_WIDTH).toChar()}"
