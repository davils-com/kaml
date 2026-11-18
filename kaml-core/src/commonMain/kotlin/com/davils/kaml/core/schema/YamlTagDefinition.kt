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

import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlTag
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A tag that a [YamlSchema] defines beyond the core tags, with the kind of node it may tag and the values it accepts.
 *
 * Definitions come from [yamlSchema] and from the standard schema [YamlSchema.Yaml11]. A definition is immutable.
 * Its matchers are caller code that kaml runs on scalars read from documents, as [YamlSchema] describes; a tag only
 * selects a definition and never constructs a type.
 *
 * @since 1.0.0
 */
public class YamlTagDefinition internal constructor(
    /**
     * The tag, such as `!ipv4` or `tag:example.com,2026:ipv4`.
     *
     * @since 1.0.0
     */
    public val tag: YamlTag,
    /**
     * The kind of node the tag may be written on; on a node of another kind it is reported as a mismatch.
     *
     * @since 1.0.0
     */
    public val kind: YamlNodeKind,
    internal val implicit: ((String) -> Boolean)?,
    internal val accepts: (String) -> Boolean,
    internal val isBuiltIn: Boolean = false,
) {
    /**
     * Whether plain scalars resolve to this tag when its implicit matcher accepts them.
     *
     * @since 1.0.0
     */
    public val isImplicit: Boolean get() = implicit != null

    override fun toString(): String = "YamlTagDefinition(${SafeText.quoted(tag.uri)}, $kind)"
}
