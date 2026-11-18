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

internal class RegexpCompiler(private val maximumSize: Int) {
    private val program = ArrayList<Instruction>()
    private var isTooLarge = false

    fun compile(node: RegexpNode): List<Instruction>? {
        emit(node)
        add(Instruction.Match)
        return program.takeIf { !isTooLarge }
    }

    private fun emit(node: RegexpNode) {
        if (isTooLarge) return
        when (node) {
            is RegexpNode.Chars -> add(Instruction.Consume(node.set))
            is RegexpNode.Sequence -> node.items.forEach(::emit)
            is RegexpNode.Alternation -> alternation(node.branches)
            is RegexpNode.Repeat -> repeat(node)
        }
    }

    private fun alternation(branches: List<RegexpNode>) {
        val jumps = ArrayList<Instruction.Jump>()
        branches.dropLast(1).forEach { branch ->
            val split = Instruction.Split(first = program.size + 1, second = 0)
            add(split)
            emit(branch)
            val jump = Instruction.Jump(0)
            add(jump)
            jumps.add(jump)
            split.second = program.size
        }
        emit(branches.last())
        jumps.forEach { jump -> jump.target = program.size }
    }

    private fun repeat(node: RegexpNode.Repeat) {
        repeat(node.least) { emit(node.node) }
        val most = node.most
        if (most == null) {
            loop(node.node)
            return
        }
        val splits = ArrayList<Instruction.Split>()
        repeat(most - node.least) {
            val split = Instruction.Split(first = program.size + 1, second = 0)
            add(split)
            splits.add(split)
            emit(node.node)
        }
        splits.forEach { split -> split.second = program.size }
    }

    private fun loop(body: RegexpNode) {
        val start = program.size
        val split = Instruction.Split(first = start + 1, second = 0)
        add(split)
        emit(body)
        add(Instruction.Jump(start))
        split.second = program.size
    }

    private fun add(instruction: Instruction) {
        if (program.size >= maximumSize) {
            isTooLarge = true
            return
        }
        program.add(instruction)
    }
}
