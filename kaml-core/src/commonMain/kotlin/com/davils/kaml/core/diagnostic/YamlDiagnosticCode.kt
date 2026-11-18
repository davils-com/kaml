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

package com.davils.kaml.core.diagnostic

import com.davils.kore.core.validation.violation.Severity

/**
 * The kind of a [YamlDiagnostic], with a stable [value] that programs may rely on and the [severity] it has by
 * default.
 *
 * The values are grouped by their prefix: `kaml.syntax` for the grammar, `kaml.directive` for directives, `kaml.tag`
 * and `kaml.schema` for tags and schemas, `kaml.alias` and `kaml.mapping` for the graph, `kaml.source` for the
 * decoding of bytes, `kaml.limit` for the limits of [com.davils.kaml.core.limit.YamlLimits] and `kaml.decode` for the
 * decoding into types of an application.
 *
 * @property value the stable, dotted identifier of the kind, such as `kaml.syntax.tab-indentation`.
 * @property severity the severity a diagnostic of this kind has by default.
 * @since 1.0.0
 */
public enum class YamlDiagnosticCode(public val value: String, public val severity: Severity) {
    /**
     * A character YAML does not allow in a document, such as a control character.
     *
     * @since 1.0.0
     */
    InvalidCharacter("kaml.syntax.invalid-character", Severity.Error),

    /**
     * Content where the grammar allows none, such as text after the end of a flow collection.
     *
     * @since 1.0.0
     */
    UnexpectedContent("kaml.syntax.unexpected-content", Severity.Error),

    /**
     * A tab in the indentation of a block, where YAML allows only spaces.
     *
     * @since 1.0.0
     */
    TabIndentation("kaml.syntax.tab-indentation", Severity.Error),

    /**
     * A line whose indentation fits no enclosing block.
     *
     * @since 1.0.0
     */
    BadIndentation("kaml.syntax.bad-indentation", Severity.Error),

    /**
     * Two parts of a node, such as a property and its content, without the white space that must separate them.
     *
     * @since 1.0.0
     */
    MissingSeparation("kaml.syntax.missing-separation", Severity.Error),

    /**
     * A quoted scalar without its closing quote.
     *
     * @since 1.0.0
     */
    UnterminatedScalar("kaml.syntax.unterminated-scalar", Severity.Error),

    /**
     * An escape of a double-quoted scalar that YAML does not define or that stands for no valid character.
     *
     * @since 1.0.0
     */
    InvalidEscape("kaml.syntax.invalid-escape", Severity.Error),

    /**
     * A document marker `---` or `...` at the start of a line inside a quoted scalar.
     *
     * @since 1.0.0
     */
    DocumentMarkerInScalar("kaml.syntax.document-marker-in-scalar", Severity.Error),

    /**
     * An implicit key that spans more than one line.
     *
     * @since 1.0.0
     */
    MultilineImplicitKey("kaml.syntax.multiline-implicit-key", Severity.Error),

    /**
     * An implicit key longer than the 1024 characters YAML allows.
     *
     * @since 1.0.0
     */
    ImplicitKeyTooLong("kaml.syntax.implicit-key-too-long", Severity.Error),

    /**
     * A header of a literal or folded scalar with an invalid indicator.
     *
     * @since 1.0.0
     */
    InvalidBlockScalarHeader("kaml.syntax.invalid-block-scalar-header", Severity.Error),

    /**
     * A literal or folded scalar whose lines are indented less than its first line or its indicator demands.
     *
     * @since 1.0.0
     */
    InvalidBlockScalarIndentation("kaml.syntax.invalid-block-scalar-indentation", Severity.Error),

    /**
     * A flow collection without its closing bracket or brace.
     *
     * @since 1.0.0
     */
    UnterminatedFlowCollection("kaml.syntax.unterminated-flow-collection", Severity.Error),

    /**
     * Two entries of a flow collection without a comma between them.
     *
     * @since 1.0.0
     */
    MissingFlowSeparator("kaml.syntax.missing-flow-separator", Severity.Error),

    /**
     * A line of a flow collection inside a block that is not indented more than the block.
     *
     * @since 1.0.0
     */
    InvalidFlowIndentation("kaml.syntax.invalid-flow-indentation", Severity.Error),

    /**
     * A tag that is malformed, such as a verbatim tag without its closing `>` or with a broken percent escape.
     *
     * @since 1.0.0
     */
    InvalidTag("kaml.syntax.invalid-tag", Severity.Error),

    /**
     * An anchor or an alias without a name or with a name an anchor cannot hold.
     *
     * @since 1.0.0
     */
    InvalidAnchor("kaml.syntax.invalid-anchor", Severity.Error),

    /**
     * An alias with an anchor or a tag of its own.
     *
     * @since 1.0.0
     */
    AliasWithProperties("kaml.syntax.alias-with-properties", Severity.Error),

    /**
     * A node with more than one anchor or more than one tag.
     *
     * @since 1.0.0
     */
    DuplicateProperty("kaml.syntax.duplicate-property", Severity.Error),

    /**
     * A malformed `%YAML` or `%TAG` directive.
     *
     * @since 1.0.0
     */
    InvalidDirective("kaml.directive.invalid", Severity.Error),

    /**
     * A second `%YAML` directive, or a second `%TAG` directive for the same handle, before one document.
     *
     * @since 1.0.0
     */
    DuplicateDirective("kaml.directive.duplicate", Severity.Error),

    /**
     * A directive YAML reserves for future use, which is ignored.
     *
     * @since 1.0.0
     */
    ReservedDirective("kaml.directive.reserved", Severity.Warning),

    /**
     * A `%YAML` directive of a major version other than 1.
     *
     * @since 1.0.0
     */
    UnsupportedVersion("kaml.directive.unsupported-version", Severity.Error),

    /**
     * A `%YAML` directive of a minor version newer than 1.2, whose document is read as YAML 1.2.
     *
     * @since 1.0.0
     */
    NewerMinorVersion("kaml.directive.newer-minor-version", Severity.Warning),

    /**
     * Directives that no `---` and no document follow.
     *
     * @since 1.0.0
     */
    DirectiveWithoutDocument("kaml.directive.without-document", Severity.Error),

    /**
     * A tag shorthand whose handle no `%TAG` directive declares.
     *
     * @since 1.0.0
     */
    UnknownTagHandle("kaml.tag.unknown-handle", Severity.Error),

    /**
     * A tag the schema does not know, when the configuration refuses unknown tags.
     *
     * @since 1.0.0
     */
    UnknownTag("kaml.tag.unknown", Severity.Error),

    /**
     * A scalar whose text is no valid value of its explicit tag, such as `!!int abc`.
     *
     * @since 1.0.0
     */
    InvalidTaggedValue("kaml.tag.invalid-value", Severity.Error),

    /**
     * A tag of one kind of node on another, such as `!!map` on a scalar.
     *
     * @since 1.0.0
     */
    TagKindMismatch("kaml.tag.kind-mismatch", Severity.Error),

    /**
     * A plain scalar the JSON schema cannot resolve, such as `yes`.
     *
     * @since 1.0.0
     */
    NotJson("kaml.schema.not-json", Severity.Error),

    /**
     * An alias to an anchor that no node before it declares.
     *
     * @since 1.0.0
     */
    UndefinedAlias("kaml.alias.undefined", Severity.Error),

    /**
     * An alias inside the node its anchor marks, which would make the graph cyclic.
     *
     * @since 1.0.0
     */
    RecursiveAlias("kaml.alias.recursive", Severity.Error),

    /**
     * A key that appears more than once in a mapping, when the configuration refuses duplicate keys.
     *
     * @since 1.0.0
     */
    DuplicateKey("kaml.mapping.duplicate-key", Severity.Error),

    /**
     * A merge key `<<` whose value is neither a mapping nor a sequence of mappings.
     *
     * @since 1.0.0
     */
    InvalidMerge("kaml.mapping.invalid-merge", Severity.Error),

    /**
     * Bytes that are no valid UTF-8, UTF-16 or UTF-32, a lone surrogate or a code point beyond U+10FFFF.
     *
     * @since 1.0.0
     */
    InvalidEncoding("kaml.source.invalid-encoding", Severity.Error),

    /**
     * A source longer than `YamlLimits.maximumInputLength`.
     *
     * @since 1.0.0
     */
    InputTooLong("kaml.limit.input-length", Severity.Error),

    /**
     * Collections nested deeper than `YamlLimits.maximumDepth`.
     *
     * @since 1.0.0
     */
    TooDeep("kaml.limit.depth", Severity.Error),

    /**
     * A stream of more nodes than `YamlLimits.maximumNodes`.
     *
     * @since 1.0.0
     */
    TooManyNodes("kaml.limit.nodes", Severity.Error),

    /**
     * Aliases that stand for more nodes in total than `YamlLimits.maximumAliasExpansion`.
     *
     * @since 1.0.0
     */
    TooManyAliasExpansions("kaml.limit.alias-expansion", Severity.Error),

    /**
     * A document with more anchors than `YamlLimits.maximumAnchors`.
     *
     * @since 1.0.0
     */
    TooManyAnchors("kaml.limit.anchors", Severity.Error),

    /**
     * A scalar longer than `YamlLimits.maximumScalarLength`.
     *
     * @since 1.0.0
     */
    ScalarTooLong("kaml.limit.scalar-length", Severity.Error),

    /**
     * A collection with more items or entries than `YamlLimits.maximumCollectionSize`.
     *
     * @since 1.0.0
     */
    CollectionTooLarge("kaml.limit.collection-size", Severity.Error),

    /**
     * A stream of more documents than `YamlLimits.maximumDocuments`.
     *
     * @since 1.0.0
     */
    TooManyDocuments("kaml.limit.documents", Severity.Error),

    /**
     * A number longer than `YamlLimits.maximumNumberLength`, which is never converted.
     *
     * @since 1.0.0
     */
    NumberTooLong("kaml.limit.number-length", Severity.Error),

    /**
     * More problems than `YamlLimits.maximumDiagnostics`; the problems beyond the limit are left out.
     *
     * @since 1.0.0
     */
    TooManyDiagnostics("kaml.limit.diagnostics", Severity.Error),

    /**
     * A node of another kind or tag than the type it is decoded into expects.
     *
     * @since 1.0.0
     */
    TypeMismatch("kaml.decode.type-mismatch", Severity.Error),

    /**
     * A mapping without a key that the type it is decoded into requires.
     *
     * @since 1.0.0
     */
    MissingProperty("kaml.decode.missing-property", Severity.Error),

    /**
     * A key of a mapping that the type it is decoded into does not know.
     *
     * @since 1.0.0
     */
    UnknownProperty("kaml.decode.unknown-property", Severity.Error),

    /**
     * A scalar that names no entry of the enum it is decoded into.
     *
     * @since 1.0.0
     */
    UnknownEnumValue("kaml.decode.unknown-enum-value", Severity.Error),

    /**
     * A polymorphic value without the tag or the key that names its type.
     *
     * @since 1.0.0
     */
    MissingPolymorphicType("kaml.decode.missing-type", Severity.Error),

    /**
     * A polymorphic value whose type names no serializer the application registered.
     *
     * @since 1.0.0
     */
    UnknownPolymorphicType("kaml.decode.unknown-type", Severity.Error),

    /**
     * A number outside the range of the type it is decoded into.
     *
     * @since 1.0.0
     */
    NumberOutOfRange("kaml.decode.number-out-of-range", Severity.Error),

    /**
     * A stream of no or several documents where exactly one is expected.
     *
     * @since 1.0.0
     */
    ExpectedSingleDocument("kaml.decode.single-document", Severity.Error),
    ;

    /**
     * Returns the stable [value] of the kind, such as `kaml.syntax.tab-indentation`.
     *
     * @return [value].
     * @since 1.0.0
     */
    override fun toString(): String = value
}
