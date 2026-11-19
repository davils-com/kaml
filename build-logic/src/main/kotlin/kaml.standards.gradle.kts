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

import java.time.Duration

plugins {
    id("kaml.base")
}

kreate {
    assurance {
        standards {
            enabled = true
            reviewEvery = Duration.ofDays(365)

            inventory {
                includes.set(setOf("*.md", "docs/**", "kaml-*/src/**"))
                excludes.set(setOf("kaml-core/src/jvmUnitTest/resources/yaml-test-suite/**"))
            }

            github("yaml/yaml-spec") {
                note = "YAML 1.2.2: the parser, composer and emitter and the failsafe, JSON and core schemas of kaml-core."
                tags = """^\d+\.\d+\.\d+$"""
            }
            github("yaml/yaml-test-suite") {
                note = "The conformance cases kaml-core runs on the JVM; THIRDPARTY.md pins the commit of its data branch."
                tags = """^v\d{4}-\d{2}-\d{2}$"""
            }
            document("YAML 1.1 types") {
                note = "The YAML 1.1 schema of kaml-core: booleans, sexagesimal and base 2 numbers, timestamps and merge keys."
                url = "https://yaml.org/type/"
            }
            rfc(3629) {
                note = "The strict UTF-8 decoder of YAML sources in kaml-core."
            }
            rfc(2781) {
                note = "The strict UTF-16 decoder of YAML sources in kaml-core."
            }
            rfc(3986) {
                note = "The percent-encoding of global tags that the emitter of kaml-core writes."
            }
            unicode {
                note = "General categories of I-Regexp and the safe display of text follow the tables of the platform."
            }
            rfc(6901) {
                note = "JSON Pointer of kaml-document."
            }
            rfc(6902) {
                note = "JSON Patch of kaml-document and the patches its diff writes."
            }
            rfc(7396) {
                note = "JSON Merge Patch of kaml-document."
            }
            rfc(9485) {
                note = "The linear-time I-Regexp engine behind match() and search() of JSONPath."
            }
            rfc(9535) {
                note = "JSONPath of kaml-document and the paths of a redaction."
            }
        }
    }
}
