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

package com.davils.kaml.core.emit

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.internal.NodeIdentity
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Collects the comments of [yamlComments] and builds them into [YamlComments].
 *
 * The builder validates every comment when it is added, so a comment that could inject YAML or hide text never
 * reaches the emitter. It is not thread-safe; it is meant to be filled by one thread.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlCommentsBuilder internal constructor() {
    private val byNode = LinkedHashMap<NodeIdentity, List<String>>()

    /**
     * Adds [text] as a comment of [node], after the comments added for it before.
     *
     * The text is split into lines at every `CR LF`, `CR` and `LF`, and each line is written as `# line`, an empty
     * line as `#`; an empty text adds nothing. Tabs are kept. A line is refused if it holds a character that could end
     * the comment or that a reader could not see: a control character other than the tab, such as `NUL`, `ESC`,
     * `DEL` or a C1 control including `NEL` `U+0085`, the line and paragraph separators `U+2028` and `U+2029`, a
     * bidirectional control such as `U+202E`, an invisible or default ignorable character such as `U+200B`, `U+00AD`,
     * a variation selector or a tag character, the byte order mark `U+FEFF`, the specials `U+FFF0`..`U+FFFB`, the
     * noncharacters `U+FFFE` and `U+FFFF`, and a surrogate without its pair. A pair of surrogates is a single
     * character and is kept.
     *
     * @param node the node the comment belongs to, compared by identity.
     * @param text the comment, which may come from anywhere.
     * @throws IllegalArgumentException if a line holds a refused character; the message names its code point as
     * `U+XXXX` and never shows the text.
     * @since 1.0.0
     */
    public fun comment(node: YamlNode, text: String) {
        val lines = CommentLines.of(text)
        if (lines.isEmpty()) return
        val key = NodeIdentity(node)
        byNode[key] = byNode[key].orEmpty() + lines
    }

    /**
     * Returns the comments added so far. Comments added later do not change the result.
     *
     * @return the comments, which cannot be changed.
     * @since 1.0.0
     */
    public fun build(): YamlComments {
        val frozen = byNode.mapValues { (_, lines) -> buildList { addAll(lines) } }
        return YamlComments(frozen)
    }
}

/**
 * Builds the comments that `Yaml.emit(document, comments)` writes, such as
 * `yamlComments { comment(root, "Generated, do not edit") }`.
 *
 * @param configure adds the comments with [YamlCommentsBuilder.comment].
 * @return the comments, which are immutable and safe to share between threads.
 * @throws IllegalArgumentException if a comment holds a character that [YamlCommentsBuilder.comment] refuses.
 * @since 1.0.0
 */
public fun yamlComments(configure: YamlCommentsBuilder.() -> Unit): YamlComments {
    val builder = YamlCommentsBuilder()
    builder.configure()
    return builder.build()
}
