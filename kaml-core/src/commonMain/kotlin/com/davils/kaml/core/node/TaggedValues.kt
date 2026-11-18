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

package com.davils.kaml.core.node

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.internal.ScalarValue
import com.davils.kaml.core.schema.YamlSchema

internal object TaggedValues {
    private val coreTags: Set<YamlTag> =
        setOf(YamlTag.STRING, YamlTag.INTEGER, YamlTag.FLOAT, YamlTag.BOOLEAN, YamlTag.NULL)

    private val numberTags: Set<YamlTag> = setOf(YamlTag.INTEGER, YamlTag.FLOAT)

    fun canonicalOf(value: String, tag: YamlTag): ScalarValue {
        val isTooLong = tag in numberTags && value.length > YamlLimits.HARD_NUMBER_LENGTH
        if (tag !in coreTags || isTooLong) return ScalarValue.Text(value)
        return requireNotNull(YamlSchema.Core.valueOf(tag, value)) { "The value is no value of the tag ${tag.uri}" }
    }
}
