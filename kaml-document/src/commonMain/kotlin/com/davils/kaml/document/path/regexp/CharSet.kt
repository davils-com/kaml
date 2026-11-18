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

package com.davils.kaml.document.path.regexp

private const val LINE_FEED: Int = '\n'.code
private const val CARRIAGE_RETURN: Int = '\r'.code

internal sealed interface CharSet {
    val weight: Int get() = 1

    fun contains(codePoint: Int): Boolean

    class Single(val codePoint: Int) : CharSet {
        override fun contains(codePoint: Int): Boolean = codePoint == this.codePoint
    }

    class Range(private val first: Int, private val last: Int) : CharSet {
        override fun contains(codePoint: Int): Boolean = codePoint in first..last
    }

    class Category(private val test: (Int) -> Boolean, private val isNegated: Boolean) : CharSet {
        override fun contains(codePoint: Int): Boolean = test(codePoint) != isNegated
    }

    class Union(private val members: List<CharSet>, private val isNegated: Boolean) : CharSet {
        override val weight: Int = members.sumOf(CharSet::weight)

        override fun contains(codePoint: Int): Boolean = members.any { member ->
            member.contains(
                codePoint,
            )
        } != isNegated
    }

    object AnyButLineBreak : CharSet {
        override fun contains(codePoint: Int): Boolean = codePoint != LINE_FEED && codePoint != CARRIAGE_RETURN
    }
}
