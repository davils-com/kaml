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
package com.davils.kaml.core.node

private const val HASH_FACTOR: Int = 31

internal class NodePair(first: YamlNode, second: YamlNode) {
    val first: YamlNode = first

    val second: YamlNode = second

    override fun equals(other: Any?): Boolean = other is NodePair && other.first === first && other.second === second

    override fun hashCode(): Int = HASH_FACTOR * NodeIdentity(first).hashCode() + NodeIdentity(second).hashCode()
}
