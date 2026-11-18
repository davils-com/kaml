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

package com.davils.kaml.core.schema

import com.davils.kaml.core.YamlViolationCodes
import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.ScalarValue
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * Decides which tag a plain scalar gets and which values a tag accepts, as the schemas of YAML 1.2 do.
 *
 * kaml offers the standard schemas [Failsafe], [Json], [Core] and [Yaml11], and [yamlSchema] extends one of them with
 * the tags of an application. A plain scalar, one written without quotes and without a tag, gets the tag of the first
 * custom tag whose implicit matcher accepts it, then `tag:yaml.org,2002:merge` if it is `<<` and [hasMergeKeys] is
 * set, and otherwise the tag the standard schema resolves. A scalar with an explicit tag must hold a value that its
 * tag accepts, or composing it reports an invalid tagged value. A number is converted only when its text is no longer
 * than the number limit of the configuration, so no number costs more than linear time.
 *
 * A schema is immutable and safe to share between threads, as far as the matchers of its custom tags are. Those
 * matchers are caller code: kaml calls them for every plain scalar and every tagged scalar of every document it
 * reads, and documents may come from an attacker. A matcher must therefore be pure, run in time linear in the length
 * of the scalar, which the scalar limit of the configuration bounds, and must not throw; an exception it throws
 * propagates unchanged and must not carry the scalar. Tags never construct types: a tag only selects a definition of
 * the schema, and kaml never instantiates a class by a name read from a document.
 *
 * @since 1.0.0
 */
public class YamlSchema internal constructor(
    /**
     * The name of the schema, such as `core`, shown escaped in diagnostics.
     *
     * @since 1.0.0
     */
    public val name: String,
    internal val resolution: ScalarResolution,
    definitions: List<YamlTagDefinition>,
    /**
     * Whether a plain `<<` is a merge key, which merges the entries of the mappings it refers to into its own
     * mapping; true for [Yaml11]. Every merged entry is charged to the alias expansion limit and counts toward the
     * collection limit of the configuration.
     *
     * @since 1.0.0
     */
    public val hasMergeKeys: Boolean,
) : Validatable {
    /**
     * The tags this schema defines beyond the core tags, in the order they were declared: the types of YAML 1.1 for
     * [Yaml11] and the tags declared by [yamlSchema]. The list is read-only.
     *
     * @since 1.0.0
     */
    public val tags: List<YamlTagDefinition> = buildList { addAll(definitions) }

    private val byTag: Map<YamlTag, YamlTagDefinition> = tags.associateBy(YamlTagDefinition::tag)
    private val implicitScalars: List<Pair<YamlTag, (String) -> Boolean>> =
        tags.mapNotNull { definition -> definition.implicit?.let { matcher -> definition.tag to matcher } }

    /**
     * Whether plain scalars resolve as in the JSON schema, where a plain scalar that is no JSON value, such as `yes`,
     * is an error instead of a string.
     *
     * @since 1.0.0
     */
    public val isJson: Boolean get() = resolution == ScalarResolution.JSON

    /**
     * Returns the definition of [tag] in this schema.
     *
     * @param tag the tag to look up.
     * @return the definition, or null if this schema does not define [tag]; the core tags have no definition.
     * @since 1.0.0
     */
    public fun definitionOf(tag: YamlTag): YamlTagDefinition? = byTag[tag]

    override fun ValidationScope.validate() {
        each(::tags, tags.filterNot(YamlTagDefinition::isBuiltIn)) { definition ->
            if (definition.tag.uri.isBlank()) fail("needs a tag such as !ipv4", YamlViolationCodes.BLANK_TAG)
            if (definition.tag.isCore) {
                fail(
                    "cannot redefine the core tag ${definition.tag.uri}",
                    YamlViolationCodes.CORE_TAG,
                )
            }
        }
        val duplicates = tags.groupBy(YamlTagDefinition::tag).filterValues { it.size > 1 }.keys
        duplicates.forEach { tag ->
            fail(::tags, "declares the tag ${tag.uri} more than once", YamlViolationCodes.DUPLICATE_TAG)
        }
    }

    override fun toString(): String = "YamlSchema(${SafeText.quoted(name)})"

    internal fun resolvePlain(text: String): YamlTag = plainTagOf(text) ?: YamlTag.STRING

    internal fun refusesPlain(text: String): Boolean = isJson && plainTagOf(text) == null

    private fun plainTagOf(text: String): YamlTag? {
        val custom = implicitScalars.firstOrNull { (_, matches) -> matches(text) }?.first
        val mergeTag = YamlTag.MERGE.takeIf { hasMergeKeys } ?: YamlTag.STRING
        val merge = mergeTag.takeIf { text == MERGE_KEY }
        return custom ?: merge ?: resolution.resolve(text)
    }

    internal fun valueOf(tag: YamlTag, text: String): ScalarValue? {
        val custom = byTag[tag]
        if (custom != null && !custom.accepts(text)) return null
        val isMergeKey = tag == YamlTag.MERGE && text == MERGE_KEY
        val isText = custom != null || isMergeKey
        return resolution.valueOf(tag, text) ?: ScalarValue.Text(text).takeIf { isText }
    }

    internal fun isKnown(tag: YamlTag, kind: YamlNodeKind): Boolean {
        val custom = byTag[tag]
        if (custom != null) return custom.kind == kind
        return coreKinds[tag] == kind
    }

    /**
     * Holds the standard schemas.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val MERGE_KEY: String = "<<"
        private const val BASE64_BLOCK: Int = 4
        private const val BASE64_EXTRAS: String = "+/="
        private const val BASE64_WHITESPACE: String = " \t\r\n"

        private val coreKinds: Map<YamlTag, YamlNodeKind> = mapOf(
            YamlTag.STRING to YamlNodeKind.Scalar,
            YamlTag.INTEGER to YamlNodeKind.Scalar,
            YamlTag.FLOAT to YamlNodeKind.Scalar,
            YamlTag.BOOLEAN to YamlNodeKind.Scalar,
            YamlTag.NULL to YamlNodeKind.Scalar,
            YamlTag.MERGE to YamlNodeKind.Scalar,
            YamlTag.SEQUENCE to YamlNodeKind.Sequence,
            YamlTag.MAPPING to YamlNodeKind.Mapping,
        )

        /**
         * The failsafe schema: every plain scalar is a string, and only strings, sequences and mappings are known.
         *
         * @since 1.0.0
         */
        public val Failsafe: YamlSchema = standard("failsafe", ScalarResolution.FAILSAFE, emptyList())

        /**
         * The JSON schema: a plain scalar must be `null`, `true`, `false` or a JSON number; any other plain scalar
         * is an error and must be quoted to be a string.
         *
         * @since 1.0.0
         */
        public val Json: YamlSchema = standard("json", ScalarResolution.JSON, emptyList())

        /**
         * The core schema of YAML 1.2 and the default: `null`, `~`, `true`, `false`, decimal, octal (`0o17`) and
         * hexadecimal (`0x1F`) integers, floats with `.inf` and `.nan` resolve to their tags; everything else is a
         * string. A float too large for a double becomes an infinity.
         *
         * @since 1.0.0
         */
        public val Core: YamlSchema = standard("core", ScalarResolution.CORE, emptyList())

        /**
         * The types of YAML 1.1 for documents that declare `%YAML 1.1`: booleans such as `yes` and `off`, binary,
         * octal, sexagesimal and underscored numbers, timestamps, `!!binary`, `!!set`, `!!omap`, `!!pairs`,
         * `!!value` and merge keys.
         *
         * @since 1.0.0
         */
        public val Yaml11: YamlSchema = standard("yaml-1.1", ScalarResolution.YAML_1_1, yaml11Tags())

        private fun standard(name: String, resolution: ScalarResolution, tags: List<YamlTagDefinition>): YamlSchema =
            YamlSchema(name = name, resolution = resolution, definitions = tags, hasMergeKeys = tags.isNotEmpty())

        private fun yaml11Tags(): List<YamlTagDefinition> = listOf(
            builtIn(YamlTag.BINARY, YamlNodeKind.Scalar, ::isBase64),
            builtIn(YamlTag.TIMESTAMP, YamlNodeKind.Scalar, ::isTimestamp),
            builtIn(YamlTag.VALUE, YamlNodeKind.Scalar) { it == "=" },
            builtIn(YamlTag.SET, YamlNodeKind.Mapping) { true },
            builtIn(YamlTag.ORDERED_MAP, YamlNodeKind.Sequence) { true },
            builtIn(YamlTag.PAIRS, YamlNodeKind.Sequence) { true },
        )

        private fun builtIn(tag: YamlTag, kind: YamlNodeKind, accepts: (String) -> Boolean): YamlTagDefinition =
            YamlTagDefinition(tag = tag, kind = kind, implicit = null, accepts = accepts, isBuiltIn = true)

        private fun isTimestamp(text: String): Boolean = Yaml11Forms.valueOf(YamlTag.TIMESTAMP, text) != null

        private fun isBase64Character(character: Char): Boolean {
            val isLetter = character in 'a'..'z' || character in 'A'..'Z'
            return isLetter || character in '0'..'9' || character in BASE64_EXTRAS
        }

        private fun isBase64(text: String): Boolean {
            val compact = text.filterNot { character -> character in BASE64_WHITESPACE }
            return compact.length % BASE64_BLOCK == 0 && compact.all(::isBase64Character)
        }
    }
}
