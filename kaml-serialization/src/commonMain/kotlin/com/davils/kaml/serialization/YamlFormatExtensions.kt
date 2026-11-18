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

package com.davils.kaml.serialization

import com.davils.kaml.core.node.YamlNode
import kotlinx.serialization.serializer

/**
 * Decodes the single document of [text] as a [T] and reports every problem instead of throwing, see
 * [YamlFormat.decodeOrFailure].
 *
 * @param T the type to decode, resolved through [YamlFormat.serializersModule].
 * @param text the YAML text, which may come from an attacker.
 * @return the decoded value with its warnings, or the problems that prevented it.
 * @throws kotlinx.serialization.SerializationException if [T] has no serializer.
 * @throws IllegalStateException if two properties of a class share a YAML name.
 * @since 1.0.0
 */
public inline fun <reified T> YamlFormat.decodeOrFailure(text: String): YamlDecoding<T> =
    decodeOrFailure(serializersModule.serializer<T>(), text)

/**
 * Decodes [node] as a [T] and reports every problem instead of throwing, see [YamlFormat.decodeOrFailure].
 *
 * @param T the type to decode, resolved through [YamlFormat.serializersModule].
 * @param node the root node, which may have been read from untrusted text or built in code.
 * @return the decoded value with its warnings, or the problems that prevented it.
 * @throws kotlinx.serialization.SerializationException if [T] has no serializer.
 * @throws IllegalStateException if two properties of a class share a YAML name.
 * @since 1.0.0
 */
public inline fun <reified T> YamlFormat.decodeOrFailure(node: YamlNode): YamlDecoding<T> =
    decodeOrFailure(serializersModule.serializer<T>(), node)

/**
 * Decodes every document of [text] as a [T], see [YamlFormat.decodeAll].
 *
 * @param T the type of each document, resolved through [YamlFormat.serializersModule].
 * @param text the YAML text, which may come from an attacker.
 * @return the decoded values, or the problems of every document if one fails.
 * @throws kotlinx.serialization.SerializationException if [T] has no serializer.
 * @throws IllegalStateException if two properties of a class share a YAML name.
 * @since 1.0.0
 */
public inline fun <reified T> YamlFormat.decodeAll(text: String): YamlDecoding<List<T>> =
    decodeAll(serializersModule.serializer<T>(), text)

/**
 * Encodes [value] to a [YamlNode], see [YamlFormat.encodeToNode].
 *
 * @param T the type of [value], resolved through [YamlFormat.serializersModule].
 * @param value the value to encode.
 * @return the root node of the encoded value.
 * @throws kotlinx.serialization.SerializationException if [T] has no serializer or under the conditions of
 * [YamlFormat.encodeToNode].
 * @throws IllegalStateException if two properties of a class share a YAML name.
 * @since 1.0.0
 */
public inline fun <reified T> YamlFormat.encodeToNode(value: T): YamlNode =
    encodeToNode(serializersModule.serializer<T>(), value)

/**
 * Decodes [node] as a [T], see [YamlFormat.decodeFromNode].
 *
 * @param T the type to decode, resolved through [YamlFormat.serializersModule].
 * @param node the root node, which may have been read from untrusted text or built in code.
 * @return the decoded value.
 * @throws YamlDecodingException if [node] cannot be decoded.
 * @throws kotlinx.serialization.SerializationException if [T] has no serializer.
 * @throws IllegalStateException if two properties of a class share a YAML name.
 * @since 1.0.0
 */
public inline fun <reified T> YamlFormat.decodeFromNode(node: YamlNode): T =
    decodeFromNode(serializersModule.serializer<T>(), node)
