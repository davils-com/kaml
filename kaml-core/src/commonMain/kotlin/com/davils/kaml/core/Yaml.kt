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

package com.davils.kaml.core

import com.davils.kaml.core.compose.YamlPipeline
import com.davils.kaml.core.emit.EventTree
import com.davils.kaml.core.emit.Representer
import com.davils.kaml.core.emit.YamlComments
import com.davils.kaml.core.emit.YamlWriter
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.event.YamlEventResult
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.YamlStream
import com.davils.kaml.core.source.YamlSource

/**
 * The entry point of kaml: it parses YAML into events or into the representation graph of [YamlNode]s and emits
 * nodes and events as YAML, all with one [configuration].
 *
 * Every input may come from an attacker. Parsing never throws for any source: malformed YAML, invalid encodings and
 * exceeded limits become diagnostics of the result. Every function checks the [YamlConfiguration.limits] before it
 * does work that grows with its input and stops at the first excess, so an oversized or hostile document costs at most
 * what the limits allow. Diagnostics, exception messages and the `toString` of every value escape the text of the
 * source, so it can neither forge a log line nor control a terminal.
 *
 * The emitter validates what it writes: comments, tags and anchors cannot inject YAML, a scalar that cannot be written
 * plain is quoted, and what it emits parses back to an equal graph.
 *
 * A [Yaml] keeps no state between calls and is safe to share between threads; [Default] is the instance with every
 * default setting.
 *
 * @property configuration the validated settings of every call.
 * @since 1.0.0
 */
public class Yaml internal constructor(public val configuration: YamlConfiguration) {
    internal val pipeline: YamlPipeline = YamlPipeline(configuration)

    /**
     * Parses [text] into its documents, as [parse] of a [YamlSource] does.
     *
     * @param text the YAML text, which may hold anything.
     * @return the documents, or the diagnostics and the partial documents of a text with errors.
     * @since 1.0.0
     */
    public fun parse(text: String): YamlParseResult = parse(YamlSource.of(text))

    /**
     * Parses [source] into its documents: it decodes, parses and composes the source and resolves every scalar with
     * the schema of the configuration.
     *
     * A source longer than `YamlLimits.maximumInputLength` is refused before it is decoded or parsed, and every other
     * limit of [YamlConfiguration.limits] stops the work at its first excess with a diagnostic, so nothing is
     * truncated silently.
     *
     * @param source the source to parse, which may hold anything.
     * @return [YamlParseResult.Parsed] for a source without errors, [YamlParseResult.Failed] otherwise.
     * @since 1.0.0
     */
    public fun parse(source: YamlSource): YamlParseResult = pipeline.parse(source)

    /**
     * Reads [source] into the events of the YAML specification, without composing a graph.
     *
     * The same limits apply as for [parse], except for those that only the composer charges, such as the expansion of
     * aliases, which events do not expand.
     *
     * @param source the source to read, which may hold anything.
     * @return the events and the diagnostics of the source.
     * @since 1.0.0
     */
    public fun events(source: YamlSource): YamlEventResult = pipeline.events(source)

    /**
     * Emits every document of [stream] as one YAML text.
     *
     * A scalar is written plain only where every reader the configuration names reads it back with its tag: the core
     * schema, the YAML 1.1 schema when `YamlEmitOptions.compatibility` is 1.1 or the document declares `%YAML 1.1`,
     * and the schema [parse] reads the document with, which is [YamlConfiguration.schema], or
     * [com.davils.kaml.core.schema.YamlSchema.Yaml11] for a document that declares `%YAML 1.1` when
     * [YamlConfiguration.isSchemaSelectedByVersion] is set. Every other scalar is quoted or tagged, so the text this
     * configuration emits it also reads back as the same graph; the implicit matchers of a custom schema are called for
     * the scalars the emitter considers writing plain.
     *
     * Anchors are written only for shared nodes, such as one that came from an alias: such a node is written once
     * with an anchor and then as aliases, and an anchor name outside `[A-Za-z0-9._-]` is replaced by a generated one.
     * A global tag with characters a URI cannot hold is written percent-encoded and reads back in that form.
     *
     * @param stream the documents to write.
     * @return the YAML text.
     * @throws IllegalStateException if the text would be longer than `YamlLimits.maximumOutputLength` of the
     * configuration; the emitter stops at the limit and returns no part of the text.
     * @throws IllegalArgumentException if a scalar holds a lone surrogate, U+FFFE or U+FFFF, if a tag or a tag prefix
     * holds a lone surrogate, if a `%TAG` handle is invalid, declared twice or has no prefix, or if a node nests as
     * deep as `YamlLimits.maximumDepth` of the configuration or deeper, which its parser would refuse.
     * @since 1.0.0
     */
    public fun emit(stream: YamlStream): String {
        val representer = Representer(configuration)
        return YamlWriter(
            configuration.emitter,
            configuration.limits,
        ).write(stream.documents.map(representer::document))
    }

    /**
     * Emits [document] as a YAML text.
     *
     * @param document the document to write.
     * @return the YAML text.
     * @throws IllegalStateException if the text would be longer than `YamlLimits.maximumOutputLength` of the
     * configuration; the emitter stops at the limit and returns no part of the text.
     * @throws IllegalArgumentException if a scalar holds a lone surrogate, U+FFFE or U+FFFF, if a tag or a tag prefix
     * holds a lone surrogate, if a `%TAG` handle is invalid, declared twice or has no prefix, or if a node nests as
     * deep as `YamlLimits.maximumDepth` of the configuration or deeper, which its parser would refuse.
     * @since 1.0.0
     */
    public fun emit(document: YamlDocument): String = emit(YamlStream(listOf(document)))

    /**
     * Emits [document] as a YAML text with the [comments] attached to its nodes, as [emit] of a [YamlStream] does.
     *
     * Comments are written for the root of the document and for the keys of block mappings only; a comment attached
     * to any other node is not written.
     *
     * @param document the document to write.
     * @param comments the comments to write before the nodes they are attached to, built with
     * [com.davils.kaml.core.emit.yamlComments], which refuses a comment that could inject YAML.
     * @return the YAML text.
     * @throws IllegalStateException if the text would be longer than `YamlLimits.maximumOutputLength` of the
     * configuration; the emitter stops at the limit and returns no part of the text.
     * @throws IllegalArgumentException if a scalar holds a lone surrogate, U+FFFE or U+FFFF, if a tag or a tag prefix
     * holds a lone surrogate, if a `%TAG` handle is invalid, declared twice or has no prefix, or if a node nests as
     * deep as `YamlLimits.maximumDepth` of the configuration or deeper, which its parser would refuse.
     * @since 1.0.0
     */
    public fun emit(document: YamlDocument, comments: YamlComments): String {
        val representer = Representer(configuration, comments)
        return YamlWriter(configuration.emitter, configuration.limits).write(listOf(representer.document(document)))
    }

    /**
     * Emits [node] as the root of a single document.
     *
     * @param node the node to write.
     * @return the YAML text.
     * @throws IllegalStateException if the text would be longer than `YamlLimits.maximumOutputLength` of the
     * configuration; the emitter stops at the limit and returns no part of the text.
     * @throws IllegalArgumentException if a scalar holds a lone surrogate, U+FFFE or U+FFFF, if a tag or a tag prefix
     * holds a lone surrogate, if a `%TAG` handle is invalid, declared twice or has no prefix, or if a node nests as
     * deep as `YamlLimits.maximumDepth` of the configuration or deeper, which its parser would refuse.
     * @since 1.0.0
     */
    public fun emit(node: YamlNode): String = emit(YamlDocument(node))

    /**
     * Emits [events] as a YAML text, such as the events [events] of a [YamlSource] returned.
     *
     * The events must form a well-formed stream: every start of a collection has its end, every node lies inside a
     * document and every alias refers to an anchor declared before it. The start and the end of the stream are
     * optional. Anchors, tags and their prefixes are written as [emit] of a [YamlStream] writes them.
     *
     * @param events the events to write, in order.
     * @return the YAML text.
     * @throws IllegalStateException if the text would be longer than `YamlLimits.maximumOutputLength` of the
     * configuration; the emitter stops at the limit and returns no part of the text.
     * @throws IllegalArgumentException if [events] do not form a well-formed sequence, if an alias refers to an
     * undefined anchor, if a scalar holds a lone surrogate, U+FFFE or U+FFFF, if a tag or a tag prefix holds a lone
     * surrogate, if a `%TAG` handle is invalid, declared twice or has no prefix, or if collections nest as deep as
     * `YamlLimits.maximumDepth` of the configuration or deeper, which its parser would refuse.
     * @since 1.0.0
     */
    public fun emitEvents(events: List<YamlEvent>): String =
        YamlWriter(configuration.emitter, configuration.limits).write(EventTree(configuration).build(events))

    /**
     * Returns the configuration, in its bounded form.
     *
     * @return a text such as `Yaml(YamlConfiguration(...))`.
     * @since 1.0.0
     */
    override fun toString(): String = "Yaml($configuration)"

    /**
     * The instance with every default setting.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * A [Yaml] with every default setting: the core schema, [com.davils.kaml.core.limit.YamlLimits.Untrusted]
         * and the default emitter options.
         *
         * @since 1.0.0
         */
        public val Default: Yaml = yaml()
    }
}
