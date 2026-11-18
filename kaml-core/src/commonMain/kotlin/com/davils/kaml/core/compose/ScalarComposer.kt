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

package com.davils.kaml.core.compose

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.internal.NodeFactory
import com.davils.kaml.core.node.internal.ScalarValue
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange

@OptIn(DavilsInternal::class)
internal class ScalarComposer(
    settings: ComposeSettings,
    private val schema: YamlSchema,
    private val report: (YamlDiagnosticCode, TextRange?, String) -> Unit,
) {
    private val tags = TagCheck(settings, schema, report)
    private val maximumNumberLength = settings.limits.maximumNumberLength

    fun scalar(event: YamlEvent.Scalar): YamlNode.Scalar {
        val tag = tagOf(event)
        return NodeFactory.scalar(
            value = event.value,
            tag = tag,
            style = event.style,
            canonical = canonicalOf(tag, event),
            anchor = event.anchor,
            range = event.range,
        )
    }

    fun empty(range: TextRange): YamlNode.Scalar {
        val event = YamlEvent.Scalar(
            range = range,
            anchor = null,
            tag = null,
            value = "",
            style = YamlScalarStyle.Plain,
        )
        return scalar(event)
    }

    private fun tagOf(event: YamlEvent.Scalar): YamlTag {
        val explicit = event.tag ?: return implicitTag(event)
        return YamlTag.STRING.takeIf { explicit.isNonSpecific } ?: explicit
    }

    private fun implicitTag(event: YamlEvent.Scalar): YamlTag {
        if (event.style != YamlScalarStyle.Plain) return YamlTag.STRING
        val resolved = schema.plainTagOf(event.value) { tag -> reportFailure(tag, event.range) }
        if (resolved == null && schema.isJson) {
            report(
                YamlDiagnosticCode.NotJson,
                event.range,
                "${SafeText.quoted(event.value)} is no JSON value; quote it to make it a string",
            )
        }
        return resolved ?: YamlTag.STRING
    }

    private fun reportFailure(tag: YamlTag, range: TextRange?) {
        val message = "The check of the tag ${SafeText.quoted(tag.uri)} failed on the value ${SafeText.REDACTED}"
        report(YamlDiagnosticCode.InvalidTaggedValue, range, message)
    }

    private fun canonicalOf(tag: YamlTag, event: YamlEvent.Scalar): ScalarValue {
        val isKnown = tags.check(tag, YamlNodeKind.Scalar, event.range)
        val isNumber = tag == YamlTag.INTEGER || tag == YamlTag.FLOAT
        if (isNumber && event.value.length > maximumNumberLength) {
            val message = "A number is longer than $maximumNumberLength characters"
            report(YamlDiagnosticCode.NumberTooLong, event.range, message)
            return ScalarValue.Text(event.value)
        }
        var hasFailed = false
        val value = schema.valueOf(tag, event.value) { failedTag ->
            hasFailed = true
            reportFailure(failedTag, event.range)
        }
        if (value == null && isKnown && !hasFailed) {
            val shownTag = SafeText.quoted(tag.uri)
            val message = "${SafeText.quoted(event.value)} is no valid value of the tag $shownTag"
            report(YamlDiagnosticCode.InvalidTaggedValue, event.range, message)
        }
        return value ?: ScalarValue.Text(event.value)
    }
}
