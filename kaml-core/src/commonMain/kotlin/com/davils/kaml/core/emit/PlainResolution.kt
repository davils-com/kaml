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

package com.davils.kaml.core.emit

import com.davils.kaml.core.YamlConfiguration
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.internal.ScalarValue
import com.davils.kaml.core.schema.YamlSchema

internal class PlainResolution(private val compatibility: YamlVersion, private val schema: YamlSchema) {
    fun resolves(text: String, tag: YamlTag): Boolean {
        val isCore = YamlSchema.Core.resolvePlain(text) == tag
        val isLegacySafe = compatibility > YamlVersion.V1_1 || YamlSchema.Yaml11.resolvePlain(text) == tag
        val isRead = schema.resolvePlain(text) == tag && !schema.refusesPlain(text)
        return isCore && isLegacySafe && isRead
    }

    fun keeps(text: String, tag: YamlTag, value: ScalarValue): Boolean {
        val isResolved = text.length <= YamlLimits.HARD_NUMBER_LENGTH && resolves(text, tag)
        return isResolved && hasValue(text, tag, value)
    }

    private fun hasValue(text: String, tag: YamlTag, value: ScalarValue): Boolean {
        val isCoreValue = YamlSchema.Core.valueOf(tag, text) == value
        val isLegacyValue = compatibility > YamlVersion.V1_1 || YamlSchema.Yaml11.valueOf(tag, text) == value
        return isCoreValue && isLegacyValue && schema.valueOf(tag, text) == value
    }

    companion object {
        fun of(configuration: YamlConfiguration, version: YamlVersion?): PlainResolution {
            val compatibility = configuration.emitter.compatibility
            val declared = version ?: compatibility
            val isLegacy = configuration.isSchemaSelectedByVersion && version == YamlVersion.V1_1
            val schema = YamlSchema.Yaml11.takeIf { isLegacy } ?: configuration.schema
            return PlainResolution(compatibility = minOf(a = compatibility, b = declared), schema = schema)
        }
    }
}
