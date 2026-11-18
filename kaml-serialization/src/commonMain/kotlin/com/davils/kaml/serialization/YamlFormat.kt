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

package com.davils.kaml.serialization

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.emit.yamlComments
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.serialization.decode.DecodeContext
import com.davils.kaml.serialization.encode.ElementOptions
import com.davils.kaml.serialization.encode.EncodeContext
import com.davils.kore.core.text.TextRange
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.SerializersModule

/**
 * The YAML format of kotlinx.serialization: encodes values to YAML text or [YamlNode]s and decodes them back, with
 * diagnostics that carry the position of every problem.
 *
 * A format is immutable and safe to use from several threads at once; every call works on state of its own. Build
 * one with [yamlFormat] or use [Default].
 *
 * Decoding treats its input as untrusted. Text is read by [YamlFormatConfiguration.yaml] within its
 * [com.davils.kaml.core.limit.YamlLimits]. The decoder applies the same limits to the node graph it walks, whether it
 * was read from text or handed in: it refuses a graph nested deeper than `maximumDepth` with
 * [YamlDiagnosticCode.TooDeep], and it counts every node it reads, a node that aliases or shared references reach
 * several times once per reference, against `maximumNodes` plus `maximumAliasExpansion`, refusing more with
 * [YamlDiagnosticCode.TooManyAliasExpansions]. At most `maximumDiagnostics` problems are collected. Two keys that
 * decode to the same property or map key are refused with [YamlDiagnosticCode.DuplicateKey]. A type name selects only
 * a serializer the application registered, see [YamlPolymorphism]. Encoding refuses a value nested deeper than
 * `maximumDepth` or made of more than `maximumNodes` nodes with a [kotlinx.serialization.SerializationException], so
 * what it writes reads back within the same limits. Encoding and decoding recurse through the serializers once per
 * level of nesting, which `maximumDepth` bounds, up to [com.davils.kaml.core.limit.YamlLimits.HARD_DEPTH].
 *
 * Numbers are checked against the type they decode to: an integer outside the range of a `Byte`, `Short`, `Int` or
 * `Long`, or a number too large for a `Float` or `Double`, is reported as [YamlDiagnosticCode.NumberOutOfRange]. An
 * infinite or not-a-number value is accepted only where the document writes it as such, as `.inf`, `-.inf` or `.nan`.
 *
 * Messages never contain raw input: names from the document are escaped with [com.davils.kore.core.text.SafeText]
 * and bounded, values are left out unless [YamlFormatBuilder.isValueQuoted] asks for them, and an
 * [IllegalArgumentException] or
 * [kotlinx.serialization.SerializationException] thrown by a serializer, such as a `require` in an `init` block, is
 * reported as [YamlDiagnosticCode.TypeMismatch] with a redacted placeholder instead of its message. Other exceptions
 * of a serializer pass through unchanged.
 *
 * @since 1.0.0
 */
public class YamlFormat internal constructor(
    /**
     * The options of this format.
     *
     * @since 1.0.0
     */
    public val configuration: YamlFormatConfiguration,
) : StringFormat {
    override val serializersModule: SerializersModule get() = configuration.serializersModule

    /**
     * Encodes [value] to YAML text, with the comments of [com.davils.kaml.serialization.annotation.YamlComment].
     *
     * @param T the type of the value.
     * @param serializer the serializer of [value].
     * @param value the value to encode.
     * @return the YAML text of a single document.
     * @throws kotlinx.serialization.SerializationException if [value] nests deeper than `maximumDepth` of the limits
     * of [YamlFormatConfiguration.yaml], encodes to more than `maximumNodes` nodes, holds a string with a lone
     * surrogate or the noncharacter U+FFFE or U+FFFF, which YAML cannot represent, or if [serializer] refuses it.
     * @throws IllegalStateException if two properties of a class share a YAML name, or a property collides with the
     * class discriminator.
     * @throws IllegalArgumentException if a [com.davils.kaml.serialization.annotation.YamlComment] contains a control,
     * separator or bidirectional character.
     * @since 1.0.0
     */
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        var root: YamlNode = yamlNull()
        val comments = yamlComments { root = encode(EncodeContext(configuration, this), serializer, value) }
        return configuration.yaml.emit(YamlDocument(root), comments)
    }

    /**
     * Encodes [value] to a [YamlNode], without comments.
     *
     * @param T the type of the value.
     * @param serializer the serializer of [value].
     * @param value the value to encode.
     * @return the root node of the encoded value.
     * @throws kotlinx.serialization.SerializationException under the same conditions as [encodeToString].
     * @throws IllegalStateException under the same conditions as [encodeToString].
     * @since 1.0.0
     */
    public fun <T> encodeToNode(serializer: SerializationStrategy<T>, value: T): YamlNode =
        encode(EncodeContext(configuration, comments = null), serializer, value)

    /**
     * Decodes the single document of [string].
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param string the YAML text, which may come from an attacker.
     * @return the decoded value; `null` for an empty stream if [deserializer] accepts it.
     * @throws YamlDecodingException if the text cannot be read, has more than one document or cannot be decoded.
     * @throws IllegalStateException if two properties of a class share a YAML name.
     * @since 1.0.0
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T =
        valueOf(decodeOrFailure(deserializer, string), string)

    /**
     * Decodes [node], which may have been read from untrusted text or built in code.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param node the root node of the value.
     * @return the decoded value.
     * @throws YamlDecodingException if [node] cannot be decoded or exceeds the limits of
     * [YamlFormatConfiguration.yaml].
     * @throws IllegalStateException if two properties of a class share a YAML name.
     * @since 1.0.0
     */
    public fun <T> decodeFromNode(deserializer: DeserializationStrategy<T>, node: YamlNode): T =
        valueOf(decodeOrFailure(deserializer, node), text = "")

    /**
     * Decodes the single document of [text] and reports every problem instead of throwing.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param text the YAML text, which may come from an attacker.
     * @return the decoded value with its warnings, or the problems that prevented it; a stream of several documents
     * fails with [YamlDiagnosticCode.ExpectedSingleDocument], an empty one decodes `null`.
     * @throws IllegalStateException if two properties of a class share a YAML name.
     * @since 1.0.0
     */
    public fun <T> decodeOrFailure(deserializer: DeserializationStrategy<T>, text: String): YamlDecoding<T> {
        val result = configuration.yaml.parse(text)
        if (result !is YamlParseResult.Parsed) return YamlDecoding.Failed(result.diagnostics)
        val documents = result.stream.documents
        val extra = documents.getOrNull(1)?.let(::severalDocuments)
        val root = documents.firstOrNull()?.root ?: yamlNull()
        val context = DecodeContext(configuration, WalkBudget.reading(limits()))
        return context.decodeRoot(deserializer, root, result.diagnostics + listOfNotNull(extra))
    }

    /**
     * Decodes [node] and reports every problem instead of throwing.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param node the root node of the value, which may have been read from untrusted text or built in code.
     * @return the decoded value with its warnings, or the problems that prevented it.
     * @throws IllegalStateException if two properties of a class share a YAML name.
     * @since 1.0.0
     */
    public fun <T> decodeOrFailure(deserializer: DeserializationStrategy<T>, node: YamlNode): YamlDecoding<T> {
        val context = DecodeContext(configuration, WalkBudget.reading(limits()))
        return context.decodeRoot(deserializer, node, emptyList())
    }

    /**
     * Decodes every document of [text] and reports every problem instead of throwing.
     *
     * The documents share one budget of nodes, so the limits bound the work for the whole stream.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of each document.
     * @param text the YAML text, which may come from an attacker.
     * @return the decoded values in the order of their documents, or the problems of every document if one fails.
     * @throws IllegalStateException if two properties of a class share a YAML name.
     * @since 1.0.0
     */
    public fun <T> decodeAll(deserializer: DeserializationStrategy<T>, text: String): YamlDecoding<List<T>> {
        val result = configuration.yaml.parse(text)
        if (result !is YamlParseResult.Parsed) return YamlDecoding.Failed(result.diagnostics)
        val budget = WalkBudget.reading(limits())
        val decodings = result.stream.documents.map { document ->
            DecodeContext(configuration, budget).decodeRoot(deserializer, document.root, emptyList())
        }
        return combined(decodings, result.diagnostics)
    }

    override fun toString(): String = "YamlFormat($configuration)"

    private fun limits(): YamlLimits = configuration.yaml.configuration.limits

    private fun <T> combined(decodings: List<YamlDecoding<T>>, earlier: List<YamlDiagnostic>): YamlDecoding<List<T>> {
        val problems = earlier + decodings.flatMap { decoding -> decoding.diagnostics }
        val decoded = decodings.filterIsInstance<YamlDecoding.Decoded<T>>()
        if (decoded.size < decodings.size) return YamlDecoding.Failed(problems)
        return YamlDecoding.Decoded(decoded.map { decoding -> decoding.value }, problems)
    }

    private fun <T> encode(context: EncodeContext, serializer: SerializationStrategy<T>, value: T): YamlNode {
        var root: YamlNode = yamlNull()
        context.encode(serializer = serializer, value = value, options = ElementOptions.NONE, depth = 0) { node ->
            root = node
        }
        return root
    }

    private fun <T> valueOf(decoding: YamlDecoding<T>, text: String): T {
        if (decoding is YamlDecoding.Decoded) return decoding.value
        throw YamlDecodingException(decoding.diagnostics, DiagnosticText.render(text, decoding.diagnostics))
    }

    private fun severalDocuments(second: YamlDocument): YamlDiagnostic = YamlDiagnostic(
        code = YamlDiagnosticCode.ExpectedSingleDocument,
        range = second.range ?: TextRange.at(0),
        message = "Expected one document, but the stream has more",
    )

    /**
     * The shared default format.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The format with every option at its default: [Yaml.Default][com.davils.kaml.core.Yaml.Default] with its
         * limits for untrusted input, unknown keys reported, defaults and nulls written, properties named as declared
         * and polymorphic types named in a `type` property.
         *
         * @since 1.0.0
         */
        public val Default: YamlFormat = yamlFormat()
    }
}
