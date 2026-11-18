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

import com.davils.kaml.document.internal.WorkBudget

internal class RegexpMachine(private val program: List<Instruction>) {
    private val size = program.size
    private var current = IntArray(size)
    private var next = IntArray(size)
    private var currentCount = 0
    private var nextCount = 0
    private val seenAt = IntArray(size) { -1 }
    private val stack = IntArray(size * 2 + 1)
    private var generation = 0
    private var steps = 0L

    fun run(subject: IntArray, isWhole: Boolean, budget: WorkBudget): Boolean? {
        currentCount = closure(0, current, 0)
        var position = 0
        var isMatched = !isWhole && hasMatch()
        var isWithinBudget = budget.spend(takeSteps())
        while (isWithinBudget && !isMatched && isRunning(position, subject.size, isWhole)) {
            advance(subject[position], isWhole)
            position++
            isMatched = !isWhole && hasMatch()
            isWithinBudget = budget.spend(takeSteps())
        }
        val isAtEnd = position == subject.size
        val isWholeMatch = isWhole && isAtEnd && hasMatch()
        val isDecided = isMatched || isAtEnd || currentCount == 0
        return (isMatched || isWholeMatch).takeIf { isDecided && isWithinBudget }
    }

    private fun isRunning(position: Int, size: Int, isWhole: Boolean): Boolean =
        position < size && (currentCount > 0 || !isWhole)

    private fun advance(codePoint: Int, isWhole: Boolean) {
        step(codePoint)
        if (!isWhole) currentCount = closure(0, current, currentCount)
    }

    private fun takeSteps(): Long {
        val taken = steps
        steps = 0L
        return taken
    }

    private fun step(codePoint: Int) {
        generation++
        nextCount = 0
        for (thread in 0 until currentCount) {
            val pc = current[thread]
            val set = (program[pc] as? Instruction.Consume)?.set
            steps += set?.weight ?: 1
            if (set != null && set.contains(codePoint)) nextCount = closure(pc + 1, next, nextCount)
        }
        val swapped = current
        current = next
        next = swapped
        currentCount = nextCount
    }

    private fun closure(start: Int, threads: IntArray, count: Int): Int {
        var added = count
        var top = 0
        stack[top++] = start
        while (top > 0) {
            val pc = stack[--top]
            steps++
            if (seenAt[pc] == generation) continue
            seenAt[pc] = generation
            when (val instruction = program[pc]) {
                is Instruction.Split -> {
                    stack[top++] = instruction.second
                    stack[top++] = instruction.first
                }

                is Instruction.Jump -> {
                    stack[top++] = instruction.target
                }

                is Instruction.Consume, Instruction.Match -> {
                    threads[added++] = pc
                }
            }
        }
        return added
    }

    private fun hasMatch(): Boolean = (0 until currentCount).any { thread ->
        program[current[thread]] ==
            Instruction.Match
    }
}
