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

package com.davils.kaml.document.internal

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.YamlDocumentLimitException

internal class JsonEquality(private val budget: WorkBudget, private val maximumDepth: Int) {
    fun isEqual(left: YamlNode, right: YamlNode): Boolean {
        val pending = ArrayDeque<Pending>()
        pending.addLast(Pending(left = left, right = right, depth = 0))
        var isEqual = true
        while (isEqual && pending.isNotEmpty()) {
            val pair = pending.removeLast()
            budget.charge()
            if (pair.depth > maximumDepth) throw YamlDocumentLimitException(DEPTH)
            isEqual = pair.left === pair.right || isShallowEqual(pair, pending)
        }
        return isEqual
    }

    fun isLess(left: YamlNode, right: YamlNode): Boolean {
        val leftScalar = left as? YamlNode.Scalar ?: return false
        return (right as? YamlNode.Scalar)?.let { rightScalar ->
            isScalarLess(
                left = leftScalar,
                right = rightScalar,
            )
        } == true
    }

    private fun isScalarLess(left: YamlNode.Scalar, right: YamlNode.Scalar): Boolean {
        val leftValue = JsonScalar.of(left)
        val rightValue = JsonScalar.of(right)
        val numbers = numberOrder(left = leftValue, right = rightValue)
        val order = numbers ?: textOrder(left = leftValue, right = rightValue)
        return order != null && order < 0
    }

    private fun numberOrder(left: JsonScalar, right: JsonScalar): Int? {
        val leftNumber = (left as? JsonScalar.Number)?.value ?: return null
        val rightNumber = (right as? JsonScalar.Number)?.value
        return rightNumber?.let(leftNumber::compareTo)
    }

    private fun textOrder(left: JsonScalar, right: JsonScalar): Int? {
        val leftText = (left as? JsonScalar.Text)?.value ?: return null
        val rightText = (right as? JsonScalar.Text)?.value
        return rightText?.let { text -> charged(left = leftText, right = text) }
    }

    private fun charged(left: String, right: String): Int {
        budget.charge(minOf(a = left.length, b = right.length).toLong())
        return CodePoints.compare(left = left, right = right)
    }

    private fun isShallowEqual(pair: Pending, pending: ArrayDeque<Pending>): Boolean = when (val left = pair.left) {
        is YamlNode.Scalar -> (pair.right as? YamlNode.Scalar)?.let { right -> isScalarEqual(left, right) } == true

        is YamlNode.Sequence -> (pair.right as? YamlNode.Sequence)?.let { right ->
            itemsQueued(left = left, right = right, depth = pair.depth + 1, pending = pending)
        } == true

        is YamlNode.Mapping -> (pair.right as? YamlNode.Mapping)?.let { right ->
            membersQueued(left = left, right = right, depth = pair.depth + 1, pending = pending)
        } == true
    }

    private fun isScalarEqual(left: YamlNode.Scalar, right: YamlNode.Scalar): Boolean {
        budget.charge(minOf(a = left.value.length, b = right.value.length).toLong())
        return JsonScalar.of(left).isEqualTo(JsonScalar.of(right))
    }

    private fun itemsQueued(
        left: YamlNode.Sequence,
        right: YamlNode.Sequence,
        depth: Int,
        pending: ArrayDeque<Pending>,
    ): Boolean {
        if (left.items.size != right.items.size) return false
        left.items.forEachIndexed { index, item ->
            pending.addLast(Pending(left = item, right = right.items[index], depth = depth))
        }
        return true
    }

    private fun membersQueued(
        left: YamlNode.Mapping,
        right: YamlNode.Mapping,
        depth: Int,
        pending: ArrayDeque<Pending>,
    ): Boolean {
        budget.charge((left.entries.size + right.entries.size).toLong())
        val leftMembers = Members.firstByToken(left)
        val rightMembers = Members.firstByToken(right)
        val isSameKeys = leftMembers.size == rightMembers.size && rightMembers.keys.containsAll(leftMembers.keys)
        if (!isSameKeys) return false
        leftMembers.forEach { (token, value) ->
            pending.addLast(Pending(left = value, right = rightMembers.getValue(token), depth = depth))
        }
        return true
    }

    private data class Pending(val left: YamlNode, val right: YamlNode, val depth: Int)

    private companion object {
        const val DEPTH: String = "maximumDepth"
    }
}
