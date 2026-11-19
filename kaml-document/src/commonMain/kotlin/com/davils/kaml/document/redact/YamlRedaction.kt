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

package com.davils.kaml.document.redact

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.YamlDocumentViolationCodes
import com.davils.kaml.document.path.JsonPath
import com.davils.kaml.document.redact.rule.GraphRedaction
import com.davils.kaml.document.redact.rule.RedactionRules
import com.davils.kaml.document.redact.rule.SyntaxRedaction
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validateOrThrow

/**
 * Removes secrets from YAML documents, from a node graph or in place from a lossless syntax tree, and fails closed.
 *
 * A node is secret when a configured JSONPath selects it or when it is the value of a member whose key a configured
 * predicate accepts; every occurrence of a duplicate key counts. A JSONPath also runs on every key that is a
 * mapping or a sequence, which a query of JSON never enters, so a secret inside such a key is found as well. A secret
 * node is replaced as a whole, and so is every other reference to it: an alias, a merge key `<<` and the anchored
 * node itself, and in a syntax tree also whatever an anchor or alias inside it stands for. Value patterns replace the
 * parts of scalar values they match, comment patterns those of comments, and the comments on the lines of a redacted
 * entry, from its key to the end of its value, go with it unless [isTrailingCommentRedacted] is off. Every
 * replacement is literal.
 *
 * A syntax tree that does not compose without errors is refused as a whole, never redacted in part, and so is one the
 * redaction cannot decide how to redact or whose redacted text would not compose again. No secret ever appears in a
 * message, an exception or a `toString` of kaml-document.
 *
 * A redaction is immutable and safe to share between threads, as long as its key predicates are.
 *
 * @since 1.0.0
 */
public class YamlRedaction internal constructor(builder: YamlRedactionBuilder) : Validatable {
    /**
     * The text that replaces whatever is redacted.
     *
     * @since 1.0.0
     */
    public val replacement: String = builder.replacement

    /**
     * The limits the redaction runs under.
     *
     * @since 1.0.0
     */
    public val limits: YamlLimits = builder.limits

    /**
     * Whether the comments on the lines of a redacted entry, from its key to the end of its value, are redacted as
     * well.
     *
     * @since 1.0.0
     */
    public val isTrailingCommentRedacted: Boolean = builder.isTrailingCommentRedacted

    /**
     * The JSONPath queries that select secret nodes, as they were configured; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val paths: List<String> = buildList { addAll(builder.paths) }

    init {
        validateOrThrow(SUBJECT)
    }

    private val rules = RedactionRules(
        replacement = replacement,
        paths = paths.map { path -> JsonPath.parse(path, limits) },
        keys = buildList { addAll(builder.keys) },
        values = buildList { addAll(builder.values) },
        comments = buildList { addAll(builder.comments) },
        limits = limits,
        isTrailingCommentRedacted = isTrailingCommentRedacted,
    )

    /**
     * Returns a copy of the graph [node] with every secret replaced by a scalar of the [replacement] and every
     * match of a value pattern replaced in its scalar.
     *
     * Secrets are found by identity, so every alias of a secret node and every member merged in through `<<` is
     * replaced too, also where a secret node is used as a key. Shared nodes stay shared in the copy.
     *
     * @param node the root of the graph, which stays unchanged.
     * @return the redacted graph.
     * @throws YamlDocumentLimitException if a query or a walk exceeds a limit, or a scalar value a value pattern
     * would have to read is longer than [YamlLimits.maximumPatternInputLength]; nothing is returned in part.
     * @throws IllegalStateException if a key predicate throws, or if redacting a key or inside a key that is a
     * collection would make two keys of one mapping equal; nothing is returned in part.
     * @since 1.0.0
     */
    public fun redact(node: YamlNode): YamlNode = GraphRedaction(rules).redacted(node)

    /**
     * Redacts every document of [tree] in place, keeping the layout, the comments and everything that is not secret.
     *
     * A redacted value is replaced by the [replacement] in double quotes; its anchor and tag stay, so aliases keep
     * resolving, now to the replacement. An alias whose anchor was removed with a redacted value is replaced as well.
     * An entry that the composer drops as a duplicate, which [com.davils.kaml.core.YamlDuplicateKeys.KeepFirst] and
     * [com.davils.kaml.core.YamlDuplicateKeys.KeepLast] allow, is invisible to every path and may be the one another
     * reader keeps, so its value and a key that is a collection are redacted whole, and so is every other entry whose
     * key has the same text. A mapping of the text that the redaction cannot match with the composed graph makes it
     * refuse with [YamlRedactionRefusal.Undecidable].
     *
     * @param tree the syntax tree, which stays unchanged.
     * @return the redacted tree with its text edits, or why it was refused.
     * @throws IllegalStateException if a key predicate throws.
     * @since 1.0.0
     */
    public fun redact(tree: YamlSyntaxTree): YamlRedactionResult = SyntaxRedaction(rules).redacted(tree)

    override fun ValidationScope.validate() {
        each(::paths, paths) { path ->
            if (JsonPath.parseOrNull(path, limits) == null) {
                fail("is not a JSONPath query within the limits", YamlDocumentViolationCodes.INVALID_PATH)
            }
        }
        if (replacement.any { it == '\n' || it == '\r' }) {
            fail(::replacement, "must not contain a line break", YamlDocumentViolationCodes.INVALID_REPLACEMENT)
        }
    }

    /**
     * Returns the replacement and the paths, escaped; never a secret, since the redaction holds none.
     *
     * @return a description such as `YamlRedaction(replacement="***", paths=["$..password"])`.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String {
        val shownPaths = paths.joinToString(
            separator = ", ",
            prefix = "[",
            postfix = "]",
        ) { path -> SafeText.quoted(path) }
        return "YamlRedaction(replacement=${SafeText.quoted(replacement)}, paths=$shownPaths)"
    }

    private companion object {
        const val SUBJECT: String = "yaml redaction"
    }
}
