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

import com.davils.kore.core.annotation.DavilsDsl

/**
 * Configures a scalar tag in the block of [YamlSchemaBuilder.scalar].
 *
 * Both functions take caller code that kaml runs on scalars of documents that may come from an attacker: it must be
 * pure, linear in the length of the scalar and must not throw, as [YamlSchema] describes. A later call replaces an
 * earlier one. The builder is not thread-safe and is read once, when the block of [YamlSchemaBuilder.scalar] returns.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlScalarTagBuilder internal constructor() {
    internal var implicit: ((String) -> Boolean)? = null
        private set

    internal var accepts: (String) -> Boolean = { true }
        private set

    /**
     * Lets plain scalars that [matches] accepts resolve to this tag.
     *
     * The implicit matchers of a schema are tried in the order their tags were declared and before the resolution of
     * the base schema; the first that accepts a scalar decides its tag.
     *
     * @param matches tells whether a plain scalar, given as its text, gets this tag.
     * @since 1.0.0
     */
    public fun implicit(matches: (String) -> Boolean) {
        implicit = matches
    }

    /**
     * Restricts the values of this tag to those [predicate] accepts; every value is accepted unless this is called.
     *
     * A scalar with this tag whose value [predicate] refuses is reported as an invalid tagged value.
     *
     * @param predicate tells whether the text of a scalar is a valid value of this tag.
     * @since 1.0.0
     */
    public fun accepts(predicate: (String) -> Boolean) {
        accepts = predicate
    }
}
