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

package com.davils.kaml.core.compose

import com.davils.kaml.core.YamlUnknownTags
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange

internal class TagCheck(
    private val settings: ComposeSettings,
    private val schema: YamlSchema,
    private val report: (YamlDiagnosticCode, TextRange?, String) -> Unit,
) {
    fun check(tag: YamlTag, kind: YamlNodeKind, range: TextRange?): Boolean {
        if (schema.isKnown(tag, kind)) return true
        val otherKind = YamlNodeKind.entries.firstOrNull { other -> other != kind && schema.isKnown(tag, other) }
        if (otherKind != null) {
            report(
                YamlDiagnosticCode.TagKindMismatch,
                range,
                "The tag ${SafeText.quoted(tag.uri)} belongs to a $otherKind, not to a $kind",
            )
        }
        val isRefused = otherKind == null && settings.unknownTags == YamlUnknownTags.Refuse
        if (isRefused) {
            report(
                YamlDiagnosticCode.UnknownTag,
                range,
                "The tag ${SafeText.quoted(tag.uri)} is not part of the schema ${SafeText.quoted(schema.name)}",
            )
        }
        return false
    }
}
