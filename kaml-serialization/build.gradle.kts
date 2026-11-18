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

plugins {
    id("kaml.serialization-library")
}

kreate {
    project {
        description = "The kotlinx.serialization format of kaml: decoding and encoding of YAML with positioned " +
            "diagnostics, polymorphism through a closed registry and the interop with kotlinx.serialization.json."
    }
}

dependencies {
    commonMainApi(project(":kaml-core"))
    commonMainApi(libs.kotlinx.serialization.core)
    commonMainApi(libs.kotlinx.serialization.json)
}
