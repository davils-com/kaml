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

package com.davils.kaml.document.fixture

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val FEW_COLLIDING_BITS: Int = 10
private const val MANY_COLLIDING_BITS: Int = 13
private const val COLLISION_GROWTH: Int = 16
private val collisionSlack: Duration = 200.milliseconds

internal fun collidingTexts(bits: Int): List<String> = (0 until (1 shl bits)).map { number ->
    (0 until bits).joinToString("") { bit -> "Aa".takeIf { number shr bit and 1 == 0 } ?: "BB" }
}

internal fun collidingMapping(bits: Int, value: (Int) -> YamlNode = { index -> yamlScalar(index) }): YamlNode.Mapping {
    val keys = collidingTexts(bits)
    return yamlMapping(keys.mapIndexed { index, key -> YamlEntry(key = yamlScalar(key), value = value(index)) })
}

private fun timed(work: () -> Unit): Duration {
    val mark = TimeSource.Monotonic.markNow()
    work()
    return mark.elapsedNow()
}

internal fun growsNearLinearly(
    fewBits: Int = FEW_COLLIDING_BITS,
    manyBits: Int = MANY_COLLIDING_BITS,
    work: (Int) -> Unit,
): Boolean {
    val few = timed { work(fewBits) }
    val many = timed { work(manyBits) }
    return many < few * COLLISION_GROWTH + collisionSlack
}
