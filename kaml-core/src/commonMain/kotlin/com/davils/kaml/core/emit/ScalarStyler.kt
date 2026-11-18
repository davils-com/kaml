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

import com.davils.kaml.core.model.YamlScalarStyle

internal class ScalarStyler(private val options: YamlEmitOptions, private val resolution: PlainResolution) {
    fun isBlock(scalar: EmitNode.Scalar, context: ScalarContext): Boolean {
        val requested = requestedStyle(scalar)
        val isBlockRequested = requested == YamlScalarStyle.Literal || requested == YamlScalarStyle.Folded
        val isBlockWanted = !context.isInline && (isBlockRequested || '\n' in scalar.value)
        return isBlockWanted && ScalarTexts.isBlockable(scalar.value) && !isPlainChosen(scalar, ScalarContext.BLOCK)
    }

    fun isEmptyBody(scalar: EmitNode.Scalar, context: ScalarContext): Boolean =
        scalar.value.isEmpty() && !isImplicitNull(scalar, context) && isPlainChosen(scalar, context)

    fun inline(scalar: EmitNode.Scalar, context: ScalarContext, out: OutputBuffer) {
        if (isImplicitNull(scalar, context)) {
            out.append(NULL_TEXT)
            return
        }
        if (isPlainChosen(scalar, context)) {
            out.append(scalar.value)
            return
        }
        quoted(scalar, out)
    }

    fun blockHeader(scalar: EmitNode.Scalar, width: Int): String = ScalarTexts.literalHeader(scalar.value, width)

    fun blockLines(scalar: EmitNode.Scalar, indent: Int, out: OutputBuffer) {
        ScalarTexts.literalLines(value = scalar.value, indent = indent, lineBreak = options.lineBreak.text, out = out)
    }

    private fun quoted(scalar: EmitNode.Scalar, out: OutputBuffer) {
        val requested = requestedStyle(scalar)
        val isSingleDefault = requested != YamlScalarStyle.DoubleQuoted && options.quoteStyle == YamlQuoteStyle.Single
        val isSinglePreferred = requested == YamlScalarStyle.SingleQuoted || isSingleDefault
        if (isSinglePreferred && ScalarTexts.isSingleQuotable(scalar.value)) {
            ScalarTexts.singleQuoted(scalar.value, out)
            return
        }
        ScalarTexts.doubleQuoted(scalar.value, out)
    }

    private fun isImplicitNull(scalar: EmitNode.Scalar, context: ScalarContext): Boolean {
        val isUntaggedEmpty = scalar.value.isEmpty() && scalar.tag == null
        return isUntaggedEmpty && !context.isEmptyAllowed && isPlainAllowed(scalar)
    }

    private fun isPlainChosen(scalar: EmitNode.Scalar, context: ScalarContext): Boolean {
        val requested = requestedStyle(scalar)
        val isQuoted = requested == YamlScalarStyle.SingleQuoted || requested == YamlScalarStyle.DoubleQuoted
        val isEmptyAllowed = scalar.value.isEmpty() && context.isEmptyAllowed
        val isSafe = isEmptyAllowed || ScalarTexts.isPlainSafe(scalar.value, context)
        return !isQuoted && isSafe && isPlainAllowed(scalar)
    }

    private fun isPlainAllowed(scalar: EmitNode.Scalar): Boolean {
        val tag = scalar.plainTag ?: return true
        return resolution.resolves(scalar.value, tag)
    }

    private fun requestedStyle(scalar: EmitNode.Scalar): YamlScalarStyle =
        scalar.style.takeIf { options.isStylePreserved } ?: YamlScalarStyle.Plain

    private companion object {
        const val NULL_TEXT: String = "null"
    }
}
