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

package com.davils.kaml.document.redact.rule

import com.davils.kore.core.text.TextRange

internal class Coverage(targets: List<TextRange>) {
    val ranges: List<TextRange> = merged(targets.filter { range -> !range.isEmpty }.sortedBy(TextRange::start))

    private val ends: Set<Int> = ranges.mapTo(HashSet(), TextRange::end)

    fun contains(range: TextRange): Boolean {
        val index = ranges.binarySearch { candidate -> candidate.start.compareTo(range.start) }
        val position = index.takeIf { it >= 0 } ?: -index - 2
        val candidate = ranges.getOrNull(position)
        return candidate != null && candidate.start <= range.start && range.end <= candidate.end
    }

    fun endsAt(offset: Int): Boolean = offset in ends

    private fun merged(sorted: List<TextRange>): List<TextRange> {
        val result = ArrayList<TextRange>()
        sorted.forEach { range ->
            val last = result.lastOrNull()?.takeIf { range.start < it.end }
            val union = last?.let { TextRange(start = it.start, end = maxOf(a = it.end, b = range.end)) }
            union?.let { result[result.lastIndex] = it } ?: result.add(range)
        }
        return result
    }
}
