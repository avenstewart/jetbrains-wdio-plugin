// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.console

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WdioOutputConverterTest {

    @Test
    fun `worker prefix stripped when content is a TeamCity ServiceMessage`() {
        val input = "[0-0] ##teamcity[testStarted nodeId='2' name='foo']\n"
        assertEquals(
            "##teamcity[testStarted nodeId='2' name='foo']\n",
            WdioOutputConverter.stripWorkerPrefix(input),
        )
    }

    @Test
    fun `worker prefix stripped when remaining content is blank`() {
        val input = "[0-0] \n"
        assertEquals("\n", WdioOutputConverter.stripWorkerPrefix(input))
    }

    @Test
    fun `worker prefix kept on lines with real log content so worker id stays visible`() {
        val input = "[0-0] RUNNING in chrome - file:///specs/example.spec.js\n"
        assertEquals(input, WdioOutputConverter.stripWorkerPrefix(input))
    }

    @Test
    fun `multi-digit worker ids are also matched`() {
        assertEquals(
            "##teamcity[testFinished nodeId='3']\n",
            WdioOutputConverter.stripWorkerPrefix("[12-7] ##teamcity[testFinished nodeId='3']\n"),
        )
    }

    @Test
    fun `unrelated bracketed prefixes are not stripped`() {
        val input = "[chromedriver] starting on port 12345\n"
        assertEquals(input, WdioOutputConverter.stripWorkerPrefix(input))
    }

    @Test
    fun `text without a worker prefix passes through unchanged`() {
        val input = "Plain log line\n"
        assertEquals(input, WdioOutputConverter.stripWorkerPrefix(input))
    }
}
