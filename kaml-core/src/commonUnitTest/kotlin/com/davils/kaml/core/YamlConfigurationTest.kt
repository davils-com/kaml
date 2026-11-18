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

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.emit.YamlEmitOptions
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val DEPTH: Int = 12

class YamlConfigurationTest : FunSpec({
    context("the configuration") {
        test("starts from safe defaults") {
            val configuration = Yaml.Default.configuration

            configuration.limits shouldBe YamlLimits.Untrusted
            configuration.emitter shouldBe YamlEmitOptions.Default
            configuration.duplicateKeys shouldBe YamlDuplicateKeys.Refuse
            configuration.unknownTags shouldBe YamlUnknownTags.Keep
            configuration.isSchemaSelectedByVersion shouldBe true
            configuration.toString() shouldContain "core"
            Yaml.Default.toString() shouldContain "Yaml"
        }

        test("refuses limits that are not positive") {
            shouldThrow<ValidationException> { yaml { limits { maximumNodes = 0 } } }
        }

        test("refuses a depth beyond the hard limit") {
            shouldThrow<ValidationException> { yaml { limits { maximumDepth = YamlLimits.HARD_DEPTH + 1 } } }
        }

        test("builds limits from a base") {
            val limits = yamlLimits(YamlLimits.Relaxed) { maximumDepth = DEPTH }

            limits.maximumDepth shouldBe DEPTH
            limits.maximumNodes shouldBe YamlLimits.Relaxed.maximumNodes
            limits.toString() shouldContain "depth=$DEPTH"
        }

        test("keeps limits set directly") {
            yaml { limits = YamlLimits.Relaxed }.configuration.limits shouldBe YamlLimits.Relaxed
        }

        test("refuses a change after the block was built") {
            var leaked: YamlBuilder? = null
            yaml { leaked = this }
            val builder = leaked

            shouldThrow<IllegalStateException> { builder?.schema = YamlSchema.Json }
            shouldThrow<IllegalStateException> { builder?.isSchemaSelectedByVersion = false }
            shouldThrow<IllegalStateException> { builder?.duplicateKeys = YamlDuplicateKeys.KeepLast }
            shouldThrow<IllegalStateException> { builder?.unknownTags = YamlUnknownTags.Refuse }
            shouldThrow<IllegalStateException> { builder?.limits = YamlLimits.Relaxed }
            shouldThrow<IllegalStateException> { builder?.emitter = YamlEmitOptions.Default }
            shouldThrow<IllegalStateException> { builder?.limits { maximumDepth = 1 } }
            shouldThrow<IllegalStateException> { builder?.emitter { indent = 1 } }
        }

        test("keeps every setting it is given") {
            val configured = yaml {
                schema = YamlSchema.Json
                isSchemaSelectedByVersion = false
                duplicateKeys = YamlDuplicateKeys.KeepFirst
                unknownTags = YamlUnknownTags.Refuse
                emitter = YamlEmitOptions.Default
            }.configuration

            configured.schema shouldBe YamlSchema.Json
            configured.isSchemaSelectedByVersion shouldBe false
            configured.duplicateKeys shouldBe YamlDuplicateKeys.KeepFirst
            configured.unknownTags shouldBe YamlUnknownTags.Refuse
        }

        test("keeps its earlier limits when a limits block is invalid") {
            shouldThrow<ValidationException> {
                yaml {
                    limits { maximumDepth = 0 }
                }
            }
        }

        test("describes the emitter") {
            YamlEmitOptions.Default.toString() shouldContain "indent=2"
        }
    }

    context("events") {
        test("report whether they are valid") {
            Yaml.Default.events(YamlSource.of("a: b")).isValid shouldBe true
            Yaml.Default.events(YamlSource.of("a: [")).isValid shouldBe false
        }
    }

    context("vocabulary") {
        test("describes anchors, tags and versions") {
            YamlAnchor("a").toString() shouldBe "&a"
            YamlTag.STRING.toString() shouldBe "<tag:yaml.org,2002:str>"
            YamlTag.NON_SPECIFIC.isNonSpecific shouldBe true
            YamlTag("!local").isLocal shouldBe true
            YamlTag.STRING.isLocal shouldBe false
            YamlVersion.V1_1.toString() shouldBe "1.1"
            (YamlVersion.V1_1 < YamlVersion.V1_2) shouldBe true
        }

        test("refuses empty anchors, tags and negative versions") {
            shouldThrow<IllegalArgumentException> { YamlAnchor("") }
            shouldThrow<IllegalArgumentException> { YamlTag("") }
            shouldThrow<IllegalArgumentException> { YamlVersion(major = -1, minor = 0) }
        }

        test("describes diagnostic codes and syntax kinds") {
            YamlDiagnosticCode.TabIndentation.toString() shouldBe "kaml.syntax.tab-indentation"
            YamlSyntaxKind.COMMENT.isTrivia shouldBe true
            YamlSyntaxKind.SCALAR.isValueNode shouldBe true
            YamlSyntaxKind.KEY.isValueNode shouldBe false
        }
    }
})
