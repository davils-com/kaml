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

package com.davils.kaml.core.event

import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.model.YamlVersion
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange

private const val NONE: String = "none"

private fun shown(value: Any?): String {
    if (value == null) return NONE
    return value.toString()
}

/**
 * An event of the serialization of YAML, as the YAML specification defines them: the stream, every document and every
 * collection start and end, and every scalar and alias lies between them.
 *
 * [com.davils.kaml.core.Yaml.events] reads a source into events and [com.davils.kaml.core.Yaml.emitEvents] writes
 * them. An event holds the text of the source as it was read, such as the value of a scalar, so its `toString`
 * escapes that text through [SafeText] and stays bounded.
 *
 * Events are immutable and safe to share between threads; two events are equal when they are of the same kind and
 * every property is equal.
 *
 * @since 1.0.0
 */
public sealed interface YamlEvent {
    /**
     * The place of the event in the source, in UTF-16 offsets of the decoded text.
     *
     * @since 1.0.0
     */
    public val range: TextRange

    /**
     * The start of the stream, the first event of every source.
     *
     * @param range the place of the start, at the start of the source.
     * @since 1.0.0
     */
    public class StreamStart(range: TextRange) : YamlEvent {
        /**
         * The place of the start, at the start of the source.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * Returns whether [other] is a start of a stream at the same place.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is StreamStart && range == other.range

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of the range.
         * @since 1.0.0
         */
        override fun hashCode(): Int = range.hashCode()

        /**
         * Returns the kind and the properties of the event.
         *
         * @return a text such as `StreamStart(range=0..0)`.
         * @since 1.0.0
         */
        override fun toString(): String = "StreamStart(range=$range)"
    }

    /**
     * The end of the stream, the last event of every source.
     *
     * @param range the place of the end, at the end of the source.
     * @since 1.0.0
     */
    public class StreamEnd(range: TextRange) : YamlEvent {
        /**
         * The place of the end, at the end of the source.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * Returns whether [other] is an end of a stream at the same place.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is StreamEnd && range == other.range

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of the range.
         * @since 1.0.0
         */
        override fun hashCode(): Int = range.hashCode()

        /**
         * Returns the kind and the properties of the event.
         *
         * @return a text such as `StreamEnd(range=4..4)`.
         * @since 1.0.0
         */
        override fun toString(): String = "StreamEnd(range=$range)"
    }

    /**
     * The start of a document, with the directives before it.
     *
     * @param range the place of the start, the marker `---` of an explicit start.
     * @param isExplicit whether the document starts with the marker `---`.
     * @param version the version of the `%YAML` directive of the document, or `null` without one.
     * @param tagDirectives the `%TAG` directives of the document, in the order of the source; the event keeps a copy.
     * @since 1.0.0
     */
    public class DocumentStart(
        range: TextRange,
        isExplicit: Boolean,
        version: YamlVersion?,
        tagDirectives: List<YamlTagDirective>,
    ) : YamlEvent {
        /**
         * The place of the start, the marker `---` of an explicit start.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * Whether the document starts with the marker `---`.
         *
         * @since 1.0.0
         */
        public val isExplicit: Boolean = isExplicit

        /**
         * The version of the `%YAML` directive of the document, or `null` without one.
         *
         * @since 1.0.0
         */
        public val version: YamlVersion? = version

        /**
         * The `%TAG` directives of the document, in the order of the source.
         *
         * @since 1.0.0
         */
        public val tagDirectives: List<YamlTagDirective> = buildList { addAll(tagDirectives) }

        private val properties: List<Any?> get() = listOf(range, isExplicit, version, tagDirectives)

        /**
         * Returns whether [other] is a start of a document with equal properties.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is DocumentStart && properties == other.properties

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of every property.
         * @since 1.0.0
         */
        override fun hashCode(): Int = properties.hashCode()

        /**
         * Returns the kind and the properties of the event.
         *
         * @return a text such as `DocumentStart(range=0..3, isExplicit=true, version=1.2, tagDirectives=0)`.
         * @since 1.0.0
         */
        override fun toString(): String =
            "DocumentStart(range=$range, isExplicit=$isExplicit, version=${shown(version)}, " +
                "tagDirectives=${tagDirectives.size})"
    }

    /**
     * The end of a document.
     *
     * @param range the place of the end, the marker `...` of an explicit end.
     * @param isExplicit whether the document ends with the marker `...`.
     * @since 1.0.0
     */
    public class DocumentEnd(
        range: TextRange,
        isExplicit: Boolean,
    ) : YamlEvent {
        /**
         * The place of the end, the marker `...` of an explicit end.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * Whether the document ends with the marker `...`.
         *
         * @since 1.0.0
         */
        public val isExplicit: Boolean = isExplicit

        private val properties: List<Any?> get() = listOf(range, isExplicit)

        /**
         * Returns whether [other] is an end of a document with equal properties.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is DocumentEnd && properties == other.properties

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of every property.
         * @since 1.0.0
         */
        override fun hashCode(): Int = properties.hashCode()

        /**
         * Returns the kind and the properties of the event.
         *
         * @return a text such as `DocumentEnd(range=8..11, isExplicit=true)`.
         * @since 1.0.0
         */
        override fun toString(): String = "DocumentEnd(range=$range, isExplicit=$isExplicit)"
    }

    /**
     * The start of a sequence; its items follow until the matching [SequenceEnd].
     *
     * @param range the place of the start, the bracket of a flow sequence.
     * @param anchor the anchor of the sequence, or `null` without one.
     * @param tag the tag the source gives the sequence, or `null` without one.
     * @param style the style the sequence is written in.
     * @since 1.0.0
     */
    public class SequenceStart(
        range: TextRange,
        anchor: YamlAnchor?,
        tag: YamlTag?,
        style: YamlCollectionStyle,
    ) : YamlEvent {
        /**
         * The place of the start, the bracket of a flow sequence.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * The anchor of the sequence, or `null` without one.
         *
         * @since 1.0.0
         */
        public val anchor: YamlAnchor? = anchor

        /**
         * The tag the source gives the sequence, or `null` without one.
         *
         * @since 1.0.0
         */
        public val tag: YamlTag? = tag

        /**
         * The style the sequence is written in.
         *
         * @since 1.0.0
         */
        public val style: YamlCollectionStyle = style

        private val properties: List<Any?> get() = listOf(range, anchor, tag, style)

        /**
         * Returns whether [other] is a start of a sequence with equal properties.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is SequenceStart && properties == other.properties

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of every property.
         * @since 1.0.0
         */
        override fun hashCode(): Int = properties.hashCode()

        /**
         * Returns the kind and the properties of the event, with the anchor and the tag escaped and bounded.
         *
         * @return a text such as `SequenceStart(range=0..1, anchor=&a, tag=none, style=Flow)`.
         * @since 1.0.0
         */
        override fun toString(): String =
            "SequenceStart(range=$range, anchor=${shown(anchor)}, tag=${shown(tag)}, style=$style)"
    }

    /**
     * The end of a sequence.
     *
     * @param range the place of the end, the bracket of a flow sequence.
     * @since 1.0.0
     */
    public class SequenceEnd(range: TextRange) : YamlEvent {
        /**
         * The place of the end, the bracket of a flow sequence.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * Returns whether [other] is an end of a sequence at the same place.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is SequenceEnd && range == other.range

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of the range.
         * @since 1.0.0
         */
        override fun hashCode(): Int = range.hashCode()

        /**
         * Returns the kind and the properties of the event.
         *
         * @return a text such as `SequenceEnd(range=5..6)`.
         * @since 1.0.0
         */
        override fun toString(): String = "SequenceEnd(range=$range)"
    }

    /**
     * The start of a mapping; its keys and values follow in turn until the matching [MappingEnd].
     *
     * @param range the place of the start, the brace of a flow mapping.
     * @param anchor the anchor of the mapping, or `null` without one.
     * @param tag the tag the source gives the mapping, or `null` without one.
     * @param style the style the mapping is written in.
     * @since 1.0.0
     */
    public class MappingStart(
        range: TextRange,
        anchor: YamlAnchor?,
        tag: YamlTag?,
        style: YamlCollectionStyle,
    ) : YamlEvent {
        /**
         * The place of the start, the brace of a flow mapping.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * The anchor of the mapping, or `null` without one.
         *
         * @since 1.0.0
         */
        public val anchor: YamlAnchor? = anchor

        /**
         * The tag the source gives the mapping, or `null` without one.
         *
         * @since 1.0.0
         */
        public val tag: YamlTag? = tag

        /**
         * The style the mapping is written in.
         *
         * @since 1.0.0
         */
        public val style: YamlCollectionStyle = style

        private val properties: List<Any?> get() = listOf(range, anchor, tag, style)

        /**
         * Returns whether [other] is a start of a mapping with equal properties.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is MappingStart && properties == other.properties

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of every property.
         * @since 1.0.0
         */
        override fun hashCode(): Int = properties.hashCode()

        /**
         * Returns the kind and the properties of the event, with the anchor and the tag escaped and bounded.
         *
         * @return a text such as `MappingStart(range=0..1, anchor=none, tag=none, style=Block)`.
         * @since 1.0.0
         */
        override fun toString(): String =
            "MappingStart(range=$range, anchor=${shown(anchor)}, tag=${shown(tag)}, style=$style)"
    }

    /**
     * The end of a mapping.
     *
     * @param range the place of the end, the brace of a flow mapping.
     * @since 1.0.0
     */
    public class MappingEnd(range: TextRange) : YamlEvent {
        /**
         * The place of the end, the brace of a flow mapping.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * Returns whether [other] is an end of a mapping at the same place.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is MappingEnd && range == other.range

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of the range.
         * @since 1.0.0
         */
        override fun hashCode(): Int = range.hashCode()

        /**
         * Returns the kind and the properties of the event.
         *
         * @return a text such as `MappingEnd(range=7..8)`.
         * @since 1.0.0
         */
        override fun toString(): String = "MappingEnd(range=$range)"
    }

    /**
     * A scalar, with its value after escapes, folding and the chomping of block scalars.
     *
     * Read from a source with errors, the value holds U+FFFD in place of a character the source cannot hold, such as
     * an escape of a lone surrogate, U+FFFE or U+FFFF; an error diagnostic of the result always reports it.
     *
     * @param range the place of the scalar in the source, its quotes included.
     * @param anchor the anchor of the scalar, or `null` without one.
     * @param tag the tag the source gives the scalar, or `null` without one; events resolve no tags.
     * @param value the value of the scalar, which may hold any character.
     * @param style the style the scalar is written in.
     * @since 1.0.0
     */
    public class Scalar(
        range: TextRange,
        anchor: YamlAnchor?,
        tag: YamlTag?,
        value: String,
        style: YamlScalarStyle,
    ) : YamlEvent {
        /**
         * The place of the scalar in the source, its quotes included.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * The anchor of the scalar, or `null` without one.
         *
         * @since 1.0.0
         */
        public val anchor: YamlAnchor? = anchor

        /**
         * The tag the source gives the scalar, or `null` without one; events resolve no tags.
         *
         * @since 1.0.0
         */
        public val tag: YamlTag? = tag

        /**
         * The value of the scalar, which may hold any character.
         *
         * @since 1.0.0
         */
        public val value: String = value

        /**
         * The style the scalar is written in.
         *
         * @since 1.0.0
         */
        public val style: YamlScalarStyle = style

        private val properties: List<Any?> get() = listOf(range, anchor, tag, value, style)

        /**
         * Returns whether [other] is a scalar with equal properties.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Scalar && properties == other.properties

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of every property.
         * @since 1.0.0
         */
        override fun hashCode(): Int = properties.hashCode()

        /**
         * Returns the kind and the properties of the event, with the value quoted and escaped through [SafeText] and
         * every part bounded.
         *
         * @return a text such as `Scalar(range=0..1, anchor=none, tag=none, value="a", style=Plain)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Scalar(range=$range, anchor=${shown(anchor)}, tag=${shown(tag)}, " +
            "value=${SafeText.quoted(value)}, style=$style)"
    }

    /**
     * An alias, which refers to the last node before it that has the same anchor.
     *
     * @param range the place of the alias in the source.
     * @param anchor the anchor the alias refers to.
     * @since 1.0.0
     */
    public class Alias(
        range: TextRange,
        anchor: YamlAnchor,
    ) : YamlEvent {
        /**
         * The place of the alias in the source.
         *
         * @since 1.0.0
         */
        override val range: TextRange = range

        /**
         * The anchor the alias refers to.
         *
         * @since 1.0.0
         */
        public val anchor: YamlAnchor = anchor

        private val properties: List<Any?> get() = listOf(range, anchor)

        /**
         * Returns whether [other] is an alias at the same place to the same anchor.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal event, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Alias && properties == other.properties

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of every property.
         * @since 1.0.0
         */
        override fun hashCode(): Int = properties.hashCode()

        /**
         * Returns the kind and the properties of the event, with the anchor escaped and bounded.
         *
         * @return a text such as `Alias(range=5..7, anchor=&a)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Alias(range=$range, anchor=$anchor)"
    }
}
