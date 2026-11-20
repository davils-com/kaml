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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.document.internal

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.boolean
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget

internal sealed interface JsonScalar {
    fun isEqualTo(other: JsonScalar, budget: WorkBudget): Boolean

    class Number(val value: NumberValue) : JsonScalar {
        override fun isEqualTo(other: JsonScalar, budget: WorkBudget): Boolean =
            other is Number && value.compareTo(other.value, budget) == 0
    }

    class Text(val value: String) : JsonScalar {
        override fun isEqualTo(other: JsonScalar, budget: WorkBudget): Boolean = other is Text && value == other.value
    }

    class Truth(val isTrue: Boolean) : JsonScalar {
        override fun isEqualTo(other: JsonScalar, budget: WorkBudget): Boolean =
            other is Truth && isTrue == other.isTrue
    }

    object Null : JsonScalar {
        override fun isEqualTo(other: JsonScalar, budget: WorkBudget): Boolean = other === Null
    }

    companion object {
        fun of(scalar: YamlNode.Scalar): JsonScalar {
            val number = NumberValue.of(scalar)?.let(::Number)
            val truth = Truth(scalar.boolean() == true).takeIf { scalar.tag == YamlTag.BOOLEAN }
            val nothing = Null.takeIf { scalar.tag == YamlTag.NULL }
            return number ?: truth ?: nothing ?: Text(scalar.value)
        }
    }
}
