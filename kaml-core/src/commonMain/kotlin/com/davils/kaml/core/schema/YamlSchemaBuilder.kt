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
package com.davils.kaml.core.schema

import com.davils.kaml.core.model.VisibleCodePoints
import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlTag
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Declares the tags of a custom [YamlSchema] in the block of [yamlSchema].
 *
 * The schema keeps the plain scalar resolution and the tags of its base schema and adds the tags declared here, in
 * the order they are declared. [yamlSchema] validates the result and refuses a blank tag, a core tag of the form
 * `tag:yaml.org,2002:...` and a tag declared twice. The matchers given to [scalar] are caller code that kaml runs on
 * scalars of documents that may come from an attacker; [YamlSchema] states what they must guarantee.
 *
 * A builder is not thread-safe; it is configured by one thread inside [yamlSchema] and closed when the schema is
 * built, after which every change throws.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlSchemaBuilder internal constructor(private val base: YamlSchema) :
    ValidatingBuilder<YamlSchema>() {
    private val definitions = ArrayList<YamlTagDefinition>()

    override val subject: String get() = "yaml schema"

    /**
     * The name of the schema, shown in diagnostics; the name of the base schema followed by `+custom` unless set.
     *
     * @throws IllegalStateException when set after the schema was built.
     * @since 1.0.0
     */
    public var name: String = "${base.name}+custom"
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a plain `<<` is a merge key; the setting of the base schema unless set.
     *
     * @throws IllegalStateException when set after the schema was built.
     * @since 1.0.0
     */
    public var hasMergeKeys: Boolean = base.hasMergeKeys
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Declares a tag for scalars.
     *
     * Without [configure], the tag accepts every value and plain scalars never resolve to it.
     *
     * @param tag the tag as it reads once its handle is resolved, such as `!ipv4` or `tag:example.com,2026:ipv4`.
     * @param configure sets the implicit matcher and the accepted values of the tag.
     * @throws IllegalStateException if the schema was built already.
     * @since 1.0.0
     */
    public fun scalar(tag: String, configure: YamlScalarTagBuilder.() -> Unit = {}) {
        checkOpen()
        val builder = YamlScalarTagBuilder()
        builder.configure()
        definitions += YamlTagDefinition(
            tag = tagOf(tag),
            kind = YamlNodeKind.Scalar,
            implicit = builder.implicit,
            accepts = builder.accepts,
        )
    }

    /**
     * Declares a tag for sequences, which accepts every sequence.
     *
     * @param tag the tag as it reads once its handle is resolved, such as `!points`.
     * @throws IllegalStateException if the schema was built already.
     * @since 1.0.0
     */
    public fun sequence(tag: String) {
        checkOpen()
        definitions += collection(tag, YamlNodeKind.Sequence)
    }

    /**
     * Declares a tag for mappings, which accepts every mapping.
     *
     * @param tag the tag as it reads once its handle is resolved, such as `!point`.
     * @throws IllegalStateException if the schema was built already.
     * @since 1.0.0
     */
    public fun mapping(tag: String) {
        checkOpen()
        definitions += collection(tag, YamlNodeKind.Mapping)
    }

    override fun assemble(): YamlSchema = YamlSchema(
        name = name,
        resolution = base.resolution,
        definitions = base.tags + definitions,
        hasMergeKeys = hasMergeKeys,
    )

    private fun collection(tag: String, kind: YamlNodeKind): YamlTagDefinition =
        YamlTagDefinition(tag = tagOf(tag), kind = kind, implicit = null, accepts = { true })

    private fun tagOf(text: String): YamlTag {
        val isTag = text.isNotEmpty() && VisibleCodePoints.isVisibleText(text)
        return YamlTag(text.takeIf { isTag } ?: BLANK)
    }

    private companion object {
        const val BLANK: String = " "
    }
}
