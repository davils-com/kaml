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

import com.davils.kaml.core.limit.YamlLimits

/**
 * Returns a copy of this node and every node below it that names no place in a source: every [YamlNode.range] of
 * the copy is `null`, while tags, styles, anchors and values stay as they are.
 *
 * A node that is shared, such as the node of an anchor and its aliases, is copied once and stays shared in the copy,
 * so detaching never expands an alias and the copy has as many nodes as the original has distinct nodes. Creating the
 * copy charges [YamlLimits.maximumNodes] of [limits] with every node it creates, before it creates it, and the walk
 * descends at most [YamlNode.depth] levels, which no node lets exceed [YamlLimits.HARD_DEPTH].
 *
 * @param limits the limits whose [YamlLimits.maximumNodes] bounds the nodes of the copy;
 * [YamlLimits.Untrusted] by default.
 * @return the copy, equal to this node.
 * @throws IllegalArgumentException if the copy would have more than [YamlLimits.maximumNodes] nodes; the message
 * names the limit and nothing of the node.
 * @since 1.0.0
 */
public fun YamlNode.detached(limits: YamlLimits = YamlLimits.Untrusted): YamlNode = Detacher(limits).copy(this)
