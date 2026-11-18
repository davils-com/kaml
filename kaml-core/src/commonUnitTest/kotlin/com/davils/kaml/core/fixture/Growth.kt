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

import io.kotest.matchers.comparables.shouldBeLessThan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

internal const val GROWTH_FACTOR: Int = 16

private const val RUNS: Int = 5
private const val SLACK: Int = 3
private val NOISE: Duration = 100.milliseconds

internal fun fastestRun(action: () -> Unit): Duration = List(RUNS) { measureTime(action) }.min()

internal infix fun Duration.shouldGrowLinearlyFrom(small: Duration) {
    this shouldBeLessThan maxOf(a = small * GROWTH_FACTOR * SLACK, b = NOISE)
}
