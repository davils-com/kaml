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

package com.davils.kaml.serialization.decode

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.serialization.PropertyNames
import com.davils.kaml.serialization.WalkBudget
import com.davils.kaml.serialization.YamlDecoding
import com.davils.kaml.serialization.YamlFormatConfiguration
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeDecoder
import kotlin.io.encoding.Base64

@OptIn(ExperimentalSerializationApi::class, DavilsInternal::class)
internal class DecodeContext(val configuration: YamlFormatConfiguration, private val budget: WalkBudget) {
    private val limits: YamlLimits = configuration.yaml.configuration.limits
    private val collected = ArrayList<YamlDiagnostic>()
    private val names = HashMap<SerialDescriptor, PropertyNames>()
    private val reading = PolymorphicReading(this)
    private val binary = ByteArraySerializer().descriptor

    val numbers: NumberReading = NumberReading(this)

    fun <T> decodeRoot(
        deserializer: DeserializationStrategy<T>,
        node: YamlNode,
        earlier: List<YamlDiagnostic>,
    ): YamlDecoding<T> = try {
        val value = root(node).decodeSerializableValue(deserializer)
        decoding(value, earlier + collected)
    } catch (failure: DecodeFailure) {
        YamlDecoding.Failed(earlier + collected + failure.diagnostic)
    }

    fun report(code: YamlDiagnosticCode, location: YamlNode, message: String) {
        val room = limits.maximumDiagnostics - collected.size
        if (room > 0) collected += diagnostic(code, location, message)
        if (room == 0) {
            val omitted = "The value has more than ${limits.maximumDiagnostics} problems; the rest are not reported"
            collected += diagnostic(YamlDiagnosticCode.TooManyDiagnostics, location, omitted)
        }
    }

    fun fail(code: YamlDiagnosticCode, location: YamlNode, message: String): Nothing =
        throw DecodeFailure(diagnostic(code, location, message))

    fun mismatch(location: YamlNode, expected: String): Nothing =
        fail(YamlDiagnosticCode.TypeMismatch, location, "Expected $expected, but found ${describeValue(location)}")

    fun visit(location: YamlNode) {
        if (budget.admits()) return
        val message = "The value expands to more than ${budget.allowance} nodes through aliases and shared nodes"
        fail(YamlDiagnosticCode.TooManyAliasExpansions, location, message)
    }

    fun enter(depth: Int, location: YamlNode) {
        if (!budget.isTooDeep(depth)) return
        fail(YamlDiagnosticCode.TooDeep, location, "The value nests deeper than ${budget.maximumDepth} levels")
    }

    fun namesOf(descriptor: SerialDescriptor): PropertyNames =
        names.getOrPut(descriptor) { PropertyNames.of(descriptor, configuration.namingStrategy) }

    fun describe(node: YamlNode): String {
        val scalar = node as? YamlNode.Scalar ?: return "a ${node.kind.name.lowercase()}"
        return SafeText.quoted(scalar.value)
    }

    fun describeValue(node: YamlNode): String {
        val scalar = node as? YamlNode.Scalar ?: return describe(node)
        return showValue(scalar.value)
    }

    fun showValue(text: String): String {
        if (!configuration.isValueQuoted) return SafeText.REDACTED
        return SafeText.quoted(text)
    }

    fun structure(descriptor: SerialDescriptor, decoder: NodeDecoder): CompositeDecoder {
        if (descriptor.kind is PolymorphicKind) return polymorphic(decoder)
        val depth = decoder.depth + 1
        enter(depth, decoder.location)
        return collection(descriptor, decoder, depth)
    }

    private fun root(node: YamlNode): NodeDecoder =
        NodeDecoder(context = this, node = node, location = node, depth = 0, isCharged = true)

    private fun polymorphic(decoder: NodeDecoder): PolymorphicDecoder {
        val typed = reading.typed(decoder)
        val depth = decoder.depth + typed.nesting
        enter(depth, decoder.location)
        return PolymorphicDecoder(context = this, typed = typed, location = decoder.location, depth = depth)
    }

    private fun collection(descriptor: SerialDescriptor, decoder: NodeDecoder, depth: Int): CompositeDecoder {
        val kind = descriptor.kind
        if (kind == StructureKind.LIST) return sequence(decoder, depth)
        val map = mapOrNull(isMap = kind == StructureKind.MAP, decoder = decoder, depth = depth)
        return map ?: ClassDecoder(
            context = this,
            descriptor = descriptor,
            node = decoder.node,
            location = decoder.location,
            depth = depth,
        )
    }

    private fun sequence(decoder: NodeDecoder, depth: Int): SequenceDecoder =
        SequenceDecoder(context = this, node = decoder.node, location = decoder.location, depth = depth)

    private fun mapOrNull(isMap: Boolean, decoder: NodeDecoder, depth: Int): MapDecoder? {
        if (!isMap) return null
        return MapDecoder(context = this, node = decoder.node, location = decoder.location, depth = depth)
    }

    private fun <T> decoding(value: T, problems: List<YamlDiagnostic>): YamlDecoding<T> {
        val isFailed = problems.any { problem -> problem.severity == Severity.Error }
        if (isFailed) return YamlDecoding.Failed(problems)
        return YamlDecoding.Decoded(value, problems)
    }

    fun refused(refusal: IllegalArgumentException, descriptor: SerialDescriptor, decoder: NodeDecoder): DecodeFailure =
        refusal as? DecodeFailure
            ?: missing(refusal, descriptor, decoder.location)
            ?: decoder.unresolvedType?.let { name -> unknownType(name, decoder.location) }
            ?: failure(YamlDiagnosticCode.TypeMismatch, decoder.location, "The value is refused: ${SafeText.REDACTED}")

    private fun missing(
        refusal: IllegalArgumentException,
        descriptor: SerialDescriptor,
        location: YamlNode,
    ): DecodeFailure? = (refusal as? MissingFieldException)?.let { missing ->
        val shown = missing.missingFields.map { field -> yamlNameOf(descriptor, field) }
        failure(YamlDiagnosticCode.MissingProperty, location, "Missing ${SafeText.text(shown.joinToString())}")
    }

    private fun yamlNameOf(descriptor: SerialDescriptor, field: String): String {
        val index = descriptor.elementNames.indexOf(field)
        if (index < 0) return field
        return namesOf(descriptor).nameOf(index)
    }

    private fun unknownType(name: String, location: YamlNode): DecodeFailure =
        failure(YamlDiagnosticCode.UnknownPolymorphicType, location, "There is no type ${SafeText.quoted(name)}")

    fun binaryOrNull(descriptor: SerialDescriptor, decoder: NodeDecoder): ByteArray? {
        val node = decoder.node
        val isBinary = descriptor == binary && node.tag == YamlTag.BINARY
        val scalar = (node as? YamlNode.Scalar)?.takeIf { isBinary } ?: return null
        val compact = scalar.value.filterNot(Char::isWhitespace)
        return base64(compact) ?: mismatch(decoder.location, "base64 data")
    }

    private fun base64(text: String): ByteArray? = try {
        Base64.decode(text)
    } catch (expected: IllegalArgumentException) {
        null
    }

    private fun failure(code: YamlDiagnosticCode, location: YamlNode, message: String): DecodeFailure =
        DecodeFailure(diagnostic(code, location, message))

    private fun diagnostic(code: YamlDiagnosticCode, location: YamlNode, message: String): YamlDiagnostic =
        YamlDiagnostic(code = code, range = location.range ?: TextRange.at(0), message = message)
}
