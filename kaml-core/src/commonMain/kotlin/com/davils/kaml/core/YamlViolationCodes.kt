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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations the validation of a configuration of kaml reports, such as a limit above its ceiling.
 *
 * They appear in the report of the [com.davils.kore.core.validation.report.ValidationException] that [yaml],
 * [com.davils.kaml.core.limit.yamlLimits] and the other builders of kaml throw for an invalid configuration.
 *
 * @since 1.0.0
 */
public object YamlViolationCodes {
    /**
     * A tag of a schema that is blank.
     *
     * @since 1.0.0
     */
    public val BLANK_TAG: ViolationCode = ViolationCode("kaml.config.blank-tag")

    /**
     * A tag that a schema defines more than once.
     *
     * @since 1.0.0
     */
    public val DUPLICATE_TAG: ViolationCode = ViolationCode("kaml.config.duplicate-tag")

    /**
     * A tag of a schema that redefines one of the tags of YAML, such as `tag:yaml.org,2002:str`.
     *
     * @since 1.0.0
     */
    public val CORE_TAG: ViolationCode = ViolationCode("kaml.config.core-tag")

    /**
     * A limit below 1.
     *
     * @since 1.0.0
     */
    public val NOT_POSITIVE: ViolationCode = ViolationCode("kaml.config.not-positive")

    /**
     * A limit above its hard ceiling of [com.davils.kaml.core.limit.YamlLimits].
     *
     * @since 1.0.0
     */
    public val ABOVE_HARD_LIMIT: ViolationCode = ViolationCode("kaml.config.above-hard-limit")

    /**
     * An indentation of the emitter outside the range it allows.
     *
     * @since 1.0.0
     */
    public val INVALID_INDENT: ViolationCode = ViolationCode("kaml.config.invalid-indent")

    /**
     * A line break the emitter cannot write.
     *
     * @since 1.0.0
     */
    public val INVALID_LINE_BREAK: ViolationCode = ViolationCode("kaml.config.invalid-line-break")
}
