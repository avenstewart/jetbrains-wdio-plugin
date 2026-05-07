// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GrepPatternTest {

    @Test
    fun `single test name with no suites yields anchored pattern`() {
        assertEquals("^baz$", buildGrepPattern(suiteNames = emptyList(), testName = "baz"))
    }

    @Test
    fun `nested suite plus test joins with spaces and anchors both ends`() {
        assertEquals(
            "^Foo Bar baz$",
            buildGrepPattern(suiteNames = listOf("Foo", "Bar"), testName = "baz"),
        )
    }

    @Test
    fun `clicking on a describe with no test name uses trailing space anchor`() {
        assertEquals(
            "^Foo Bar ",
            buildGrepPattern(suiteNames = listOf("Foo", "Bar"), testName = null),
        )
    }

    @Test
    fun `regex metacharacters in names are escaped`() {
        val pattern = buildGrepPattern(
            suiteNames = listOf("a.b+c*"),
            testName = "(d)[e]{f}|g?\\h\$i^",
        )
        assertEquals("^a\\.b\\+c\\* \\(d\\)\\[e\\]\\{f\\}\\|g\\?\\\\h\\\$i\\^$", pattern)
    }
}
