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

private const val FIRST_NONCHARACTER: Int = 0xFFFE

internal object ScalarText {
    fun isRepresentable(text: String): Boolean = text.indices.none { index -> isRefused(text, index) }

    private fun isRefused(text: String, index: Int): Boolean {
        val character = text[index]
        val isLoneHigh = character.isHighSurrogate() && text.getOrNull(index + 1)?.isLowSurrogate() != true
        val isLoneLow = character.isLowSurrogate() && text.getOrNull(index - 1)?.isHighSurrogate() != true
        return character.code >= FIRST_NONCHARACTER || isLoneHigh || isLoneLow
    }
}
