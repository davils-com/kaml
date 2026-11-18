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

package com.davils.kaml.document

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations the builders of kaml-document report, such as those of
 * [com.davils.kaml.document.redact.yamlRedaction] and [com.davils.kaml.document.merge.yamlMergeOptions].
 *
 * @since 1.0.0
 */
public object YamlDocumentViolationCodes {
    /**
     * A configured JSONPath query is not valid or exceeds its limits.
     *
     * @since 1.0.0
     */
    public val INVALID_PATH: ViolationCode = ViolationCode("kaml.document.invalid-path")

    /**
     * A configured JSON pointer is not valid.
     *
     * @since 1.0.0
     */
    public val INVALID_POINTER: ViolationCode = ViolationCode("kaml.document.invalid-pointer")

    /**
     * A configured replacement holds a line break.
     *
     * @since 1.0.0
     */
    public val INVALID_REPLACEMENT: ViolationCode = ViolationCode("kaml.document.invalid-replacement")

    /**
     * A configured key that identifies the items of a sequence is blank.
     *
     * @since 1.0.0
     */
    public val INVALID_KEY: ViolationCode = ViolationCode("kaml.document.invalid-key")
}
