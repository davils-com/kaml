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

package com.davils.kaml.serialization.fixture

import com.davils.kaml.serialization.annotation.YamlComment
import kotlinx.serialization.Serializable

@Serializable
internal data class Server(
    @YamlComment("The host name of the server")
    val host: String,
    val listeners: List<Listener> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
    val weight: Double = 1.0,
    val isEnabled: Boolean = true,
    val owner: String? = null,
)
