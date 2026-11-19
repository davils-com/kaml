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
package com.davils.kaml.core.event

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.everyHostileFragment
import com.davils.kaml.core.fixture.hostileText
import com.davils.kaml.core.fixture.shouldShowSafely
import com.davils.kaml.core.fixture.visibleTagOf
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.model.YamlVersion
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.checkAll

private const val OTHER_OFFSET: Int = 9
private const val VALUE: String = "value"

private val here: TextRange = TextRange(start = 0, end = 1)
private val there: TextRange = TextRange.at(OTHER_OFFSET)
private val knownAnchor: YamlAnchor = YamlAnchor("a")
private val otherAnchor: YamlAnchor = YamlAnchor("b")
private val knownTag: YamlTag = YamlTag("!t")
private val directive: YamlTagDirective = YamlTagDirective(handle = "!e!", prefix = "tag:e,2000:")

private fun sequenceStart(
    range: TextRange = here,
    anchor: YamlAnchor? = knownAnchor,
    tag: YamlTag? = knownTag,
    style: YamlCollectionStyle = YamlCollectionStyle.Flow,
): YamlEvent = YamlEvent.SequenceStart(range = range, anchor = anchor, tag = tag, style = style)

private fun mappingStart(
    range: TextRange = here,
    anchor: YamlAnchor? = knownAnchor,
    tag: YamlTag? = knownTag,
    style: YamlCollectionStyle = YamlCollectionStyle.Flow,
): YamlEvent = YamlEvent.MappingStart(range = range, anchor = anchor, tag = tag, style = style)

private fun scalar(
    range: TextRange = here,
    anchor: YamlAnchor? = knownAnchor,
    tag: YamlTag? = knownTag,
    value: String = VALUE,
    style: YamlScalarStyle = YamlScalarStyle.Plain,
): YamlEvent = YamlEvent.Scalar(range = range, anchor = anchor, tag = tag, value = value, style = style)

private fun documentStart(
    range: TextRange = here,
    isExplicit: Boolean = true,
    version: YamlVersion? = YamlVersion.V1_2,
    tagDirectives: List<YamlTagDirective> = listOf(directive),
): YamlEvent = YamlEvent.DocumentStart(
    range = range,
    isExplicit = isExplicit,
    version = version,
    tagDirectives = tagDirectives,
)

private fun shouldCompareByEveryProperty(event: YamlEvent, same: YamlEvent, others: List<YamlEvent>) {
    val anything: Any = event

    event shouldBe same
    event.hashCode() shouldBe same.hashCode()
    others.forEach { other -> event shouldNotBe other }
    anything shouldNotBe event.toString()
}

class YamlEventTest : FunSpec({
    context("equality") {
        test("compares the boundaries of streams by their place") {
            shouldCompareByEveryProperty(
                event = YamlEvent.StreamStart(here),
                same = YamlEvent.StreamStart(here),
                others = listOf(YamlEvent.StreamStart(there), YamlEvent.StreamEnd(here)),
            )
            shouldCompareByEveryProperty(
                event = YamlEvent.StreamEnd(here),
                same = YamlEvent.StreamEnd(here),
                others = listOf(YamlEvent.StreamEnd(there), YamlEvent.StreamStart(here)),
            )
        }

        test("compares the starts of documents by every property") {
            shouldCompareByEveryProperty(
                event = documentStart(),
                same = documentStart(),
                others = listOf(
                    documentStart(range = there),
                    documentStart(isExplicit = false),
                    documentStart(version = null),
                    documentStart(tagDirectives = emptyList()),
                    YamlEvent.StreamStart(here),
                ),
            )
        }

        test("compares the ends of documents by every property") {
            shouldCompareByEveryProperty(
                event = YamlEvent.DocumentEnd(range = here, isExplicit = true),
                same = YamlEvent.DocumentEnd(range = here, isExplicit = true),
                others = listOf(
                    YamlEvent.DocumentEnd(range = there, isExplicit = true),
                    YamlEvent.DocumentEnd(range = here, isExplicit = false),
                    YamlEvent.StreamEnd(here),
                ),
            )
        }

        test("compares the starts of sequences by every property") {
            shouldCompareByEveryProperty(
                event = sequenceStart(),
                same = sequenceStart(),
                others = listOf(
                    sequenceStart(range = there),
                    sequenceStart(anchor = otherAnchor),
                    sequenceStart(anchor = null),
                    sequenceStart(tag = null),
                    sequenceStart(style = YamlCollectionStyle.Block),
                    mappingStart(),
                ),
            )
        }

        test("compares the starts of mappings by every property") {
            shouldCompareByEveryProperty(
                event = mappingStart(),
                same = mappingStart(),
                others = listOf(
                    mappingStart(range = there),
                    mappingStart(anchor = otherAnchor),
                    mappingStart(anchor = null),
                    mappingStart(tag = null),
                    mappingStart(style = YamlCollectionStyle.Block),
                    sequenceStart(),
                ),
            )
        }

        test("compares the ends of collections by their place") {
            shouldCompareByEveryProperty(
                event = YamlEvent.SequenceEnd(here),
                same = YamlEvent.SequenceEnd(here),
                others = listOf(YamlEvent.SequenceEnd(there), YamlEvent.MappingEnd(here)),
            )
            shouldCompareByEveryProperty(
                event = YamlEvent.MappingEnd(here),
                same = YamlEvent.MappingEnd(here),
                others = listOf(YamlEvent.MappingEnd(there), YamlEvent.SequenceEnd(here)),
            )
        }

        test("compares scalars by every property") {
            shouldCompareByEveryProperty(
                event = scalar(),
                same = scalar(),
                others = listOf(
                    scalar(range = there),
                    scalar(anchor = otherAnchor),
                    scalar(anchor = null),
                    scalar(tag = null),
                    scalar(value = "other"),
                    scalar(style = YamlScalarStyle.Literal),
                    YamlEvent.Alias(range = here, anchor = knownAnchor),
                ),
            )
        }

        test("compares aliases by every property") {
            shouldCompareByEveryProperty(
                event = YamlEvent.Alias(range = here, anchor = knownAnchor),
                same = YamlEvent.Alias(range = here, anchor = knownAnchor),
                others = listOf(
                    YamlEvent.Alias(range = there, anchor = knownAnchor),
                    YamlEvent.Alias(range = here, anchor = otherAnchor),
                    scalar(),
                ),
            )
        }
    }

    context("the text forms") {
        test("name the kind and the properties of every event") {
            YamlEvent.StreamStart(here).toString() shouldBe "StreamStart(range=0..1)"
            YamlEvent.StreamEnd(here).toString() shouldBe "StreamEnd(range=0..1)"
            documentStart().toString() shouldBe
                "DocumentStart(range=0..1, isExplicit=true, version=1.2, tagDirectives=1)"
            documentStart(version = null).toString() shouldBe
                "DocumentStart(range=0..1, isExplicit=true, version=none, tagDirectives=1)"
            YamlEvent.DocumentEnd(range = here, isExplicit = false).toString() shouldBe
                "DocumentEnd(range=0..1, isExplicit=false)"
            sequenceStart().toString() shouldBe "SequenceStart(range=0..1, anchor=&a, tag=<!t>, style=Flow)"
            mappingStart(anchor = null, tag = null).toString() shouldBe
                "MappingStart(range=0..1, anchor=none, tag=none, style=Flow)"
            sequenceStart(anchor = null, tag = null).toString() shouldBe
                "SequenceStart(range=0..1, anchor=none, tag=none, style=Flow)"
            scalar(anchor = null, tag = null).toString() shouldBe
                "Scalar(range=0..1, anchor=none, tag=none, value=\"value\", style=Plain)"
            YamlEvent.SequenceEnd(here).toString() shouldBe "SequenceEnd(range=0..1)"
            YamlEvent.MappingEnd(here).toString() shouldBe "MappingEnd(range=0..1)"
            scalar().toString() shouldBe "Scalar(range=0..1, anchor=&a, tag=<!t>, value=\"value\", style=Plain)"
            YamlEvent.Alias(range = here, anchor = knownAnchor).toString() shouldBe "Alias(range=0..1, anchor=&a)"
        }

        test("escape a hostile value and a hostile tag") {
            scalar(value = everyHostileFragment, tag = visibleTagOf(everyHostileFragment)).shouldShowSafely()
            checkAll(hostileText) { text ->
                scalar(value = text).shouldShowSafely()
                sequenceStart(tag = visibleTagOf(text)).shouldShowSafely()
                mappingStart(tag = visibleTagOf(text)).shouldShowSafely()
            }
        }
    }

    context("the start of a document") {
        test("keeps a copy of its directives") {
            val directives = mutableListOf(directive)
            val start = YamlEvent.DocumentStart(
                range = here,
                isExplicit = false,
                version = null,
                tagDirectives = directives,
            )
            directives.clear()

            start.tagDirectives shouldBe listOf(directive)
        }
    }

    context("a result of events") {
        val events = listOf(YamlEvent.StreamStart(here), YamlEvent.StreamEnd(here))
        val warning = YamlDiagnostic(YamlDiagnosticCode.ReservedDirective, here, "A reserved directive")
        val error = YamlDiagnostic(YamlDiagnosticCode.InvalidTag, here, "A broken tag")

        test("is valid with warnings alone") {
            YamlEventResult(events, listOf(warning)).isValid shouldBe true
            YamlEventResult(events, listOf(warning, error)).isValid shouldBe false
            error.severity shouldBe Severity.Error
        }

        test("keeps copies of its events and diagnostics") {
            val held = events.toMutableList()
            val found = mutableListOf(warning)
            val result = YamlEventResult(held, found)
            held.clear()
            found.clear()

            result.events shouldBe events
            result.diagnostics shouldBe listOf(warning)
        }

        test("compares its events and diagnostics") {
            val result = YamlEventResult(events, listOf(warning))
            val anything: Any = result

            result shouldBe YamlEventResult(events, listOf(warning))
            result.hashCode() shouldBe YamlEventResult(events, listOf(warning)).hashCode()
            result shouldNotBe YamlEventResult(emptyList(), listOf(warning))
            result shouldNotBe YamlEventResult(events, emptyList())
            anything shouldNotBe events
        }

        test("shows only the numbers of events and diagnostics") {
            YamlEventResult(events, listOf(warning)).toString() shouldBe "YamlEventResult(events=2, diagnostics=1)"
        }
    }
})
