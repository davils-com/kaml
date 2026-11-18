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

package com.davils.kaml.core.conformance.fixture

private const val ROOT: String = "/yaml-test-suite"

internal object SuiteCases {
    val all: List<SuiteCase> by lazy {
        val index = resource("cases.txt").orEmpty()
        val ids = index.lines().filter(String::isNotBlank)
        ids.map(::load)
    }

    private fun load(id: String): SuiteCase = SuiteCase(
        id = id,
        name = text("$id/===").trim(),
        yaml = text("$id/in.yaml"),
        events = resource("$id/test.event"),
        json = resource("$id/in.json"),
        isError = resource("$id/error") != null,
    )

    private fun text(path: String): String = resource(path).orEmpty()

    private fun resource(path: String): String? {
        val type = SuiteCases::class.java
        val stream = type.getResourceAsStream("$ROOT/$path") ?: return null
        return stream.use { input -> input.readBytes().decodeToString() }
    }
}
