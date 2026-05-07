// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdapterArgvTest {

    @Test
    fun `Mocha emits no flags when filter is None and not debugging`() {
        assertEquals(emptyList<String>(), MochaAdapter.argvFor(TestFilter.None, debug = false))
    }

    @Test
    fun `Mocha emits grep flag with the supplied pattern`() {
        val args = MochaAdapter.argvFor(TestFilter.Grep("^Foo bar$"), debug = false)
        assertEquals(listOf("--mochaOpts.grep", "^Foo bar$"), args)
    }

    @Test
    fun `Mocha debug mode emits timeout zero`() {
        val args = MochaAdapter.argvFor(TestFilter.None, debug = true)
        assertEquals(listOf("--mochaOpts.timeout", "0"), args)
    }

    @Test
    fun `Mocha grep and debug combine`() {
        val args = MochaAdapter.argvFor(TestFilter.Grep("^x$"), debug = true)
        assertEquals(listOf("--mochaOpts.grep", "^x$", "--mochaOpts.timeout", "0"), args)
    }

    @Test
    fun `Jasmine uses jasmineOpts grep flag`() {
        val args = JasmineAdapter.argvFor(TestFilter.Grep("^x$"), debug = false)
        assertEquals(listOf("--jasmineOpts.grep", "^x$"), args)
    }

    @Test
    fun `Jasmine debug mode uses defaultTimeoutInterval`() {
        val args = JasmineAdapter.argvFor(TestFilter.None, debug = true)
        assertEquals(listOf("--jasmineOpts.defaultTimeoutInterval", "0"), args)
    }
}
