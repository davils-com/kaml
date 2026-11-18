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
 * What the composer does with a key that appears more than once in one mapping.
 *
 * The same rule applies wherever kaml composes a mapping. A mapping built in code, with
 * [com.davils.kaml.core.node.yamlMapping] or [com.davils.kaml.core.node.withEntries], refuses a repeated key
 * whatever the configuration says.
 *
 * @since 1.0.0
 */
public enum class YamlDuplicateKeys {
    /**
     * A repeated key is an error, as YAML demands: it is reported as a
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.DuplicateKey] diagnostic and the source fails to parse.
     *
     * @since 1.0.0
     */
    Refuse,

    /**
     * The first entry of a repeated key is kept and the later ones are dropped, without a diagnostic.
     *
     * @since 1.0.0
     */
    KeepFirst,

    /**
     * The last entry of a repeated key is kept and the earlier ones are dropped, without a diagnostic.
     *
     * @since 1.0.0
     */
    KeepLast,
}
