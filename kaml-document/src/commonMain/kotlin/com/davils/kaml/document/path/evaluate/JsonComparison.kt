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

package com.davils.kaml.document.path.evaluate

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.internal.JsonEquality
import com.davils.kaml.document.path.filter.ComparisonOperator

internal class JsonComparison(private val equality: JsonEquality) {
    fun compare(left: YamlNode?, operator: ComparisonOperator, right: YamlNode?): Boolean = when (operator) {
        ComparisonOperator.Equal -> isEqual(left, right)
        ComparisonOperator.NotEqual -> !isEqual(left, right)
        ComparisonOperator.Less -> isLess(left, right)
        ComparisonOperator.Greater -> isLess(left = right, right = left)
        ComparisonOperator.LessOrEqual -> isLess(left, right) || isEqual(left, right)
        ComparisonOperator.GreaterOrEqual -> isLess(left = right, right = left) || isEqual(left, right)
    }

    private fun isEqual(left: YamlNode?, right: YamlNode?): Boolean {
        if (left == null || right == null) return left == null && right == null
        return equality.isEqual(left, right)
    }

    private fun isLess(left: YamlNode?, right: YamlNode?): Boolean =
        left != null && right != null && equality.isLess(left, right)
}
