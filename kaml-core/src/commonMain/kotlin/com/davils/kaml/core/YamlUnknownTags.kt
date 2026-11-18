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

/**
 * What the composer does with a tag that the schema does not define, such as `!point`.
 *
 * A tag only names a type: kaml never constructs an object from a tag, whatever this setting says.
 *
 * @since 1.0.0
 */
public enum class YamlUnknownTags {
    /**
     * An unknown tag is kept on its node, which keeps its text as its value.
     *
     * @since 1.0.0
     */
    Keep,

    /**
     * An unknown tag is an error, reported as a [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.UnknownTag]
     * diagnostic.
     *
     * @since 1.0.0
     */
    Refuse,
}
