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

internal class OutputBuffer(private val limit: Int, private val isStrict: Boolean = true) {
    private val text = StringBuilder()

    var isOverflowed: Boolean = false
        private set

    fun append(part: String) {
        if (reserve(part.length)) text.append(part)
    }

    fun append(character: Char) {
        if (reserve(1)) text.append(character)
    }

    fun append(source: String, start: Int, end: Int) {
        if (reserve(end - start)) text.append(source, start, end)
    }

    fun spaces(count: Int) {
        if (reserve(count)) repeat(count) { text.append(' ') }
    }

    override fun toString(): String = text.toString()

    private fun reserve(count: Int): Boolean {
        val hasRoom = !isOverflowed && count <= limit - text.length
        if (hasRoom) return true
        check(!isStrict) { TOO_LONG }
        isOverflowed = true
        return false
    }

    companion object {
        const val TOO_LONG: String = "The emitted YAML is longer than the maximum output length of the limits"
    }
}
