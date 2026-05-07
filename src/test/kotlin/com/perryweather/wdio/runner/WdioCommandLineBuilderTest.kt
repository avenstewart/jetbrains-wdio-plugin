// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.perryweather.wdio.config.WdioRunSettings
import com.perryweather.wdio.framework.JasmineAdapter
import com.perryweather.wdio.framework.MochaAdapter
import com.perryweather.wdio.framework.TestFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WdioCommandLineBuilderTest {

    private val wdioMainJs = "/abs/node_modules/@wdio/cli/bin/wdio.js"

    @Test
    fun `simple Mocha file run produces the expected argv`() {
        val settings = WdioRunSettings(
            wdioConfigFilePath = "wdio.conf.js",
            testFilePath = "/abs/test/foo.spec.js",
        )
        val cl = WdioCommandLineBuilder.build(settings, MochaAdapter, wdioMainJs, debug = false)
        assertEquals(
            listOf(
                wdioMainJs,
                "run",
                "wdio.conf.js",
                "--framework",
                "mocha",
                "--spec",
                "/abs/test/foo.spec.js",
            ),
            cl.parametersList.parameters,
        )
    }

    @Test
    fun `Mocha grep filter is inserted between framework and spec`() {
        val settings = WdioRunSettings(
            wdioConfigFilePath = "wdio.conf.js",
            testFilePath = "/abs/test/foo.spec.js",
            testFilter = TestFilter.Grep("^Foo bar$"),
        )
        val cl = WdioCommandLineBuilder.build(settings, MochaAdapter, wdioMainJs, debug = false)
        assertEquals(
            listOf(
                wdioMainJs,
                "run",
                "wdio.conf.js",
                "--framework",
                "mocha",
                "--mochaOpts.grep",
                "^Foo bar$",
                "--spec",
                "/abs/test/foo.spec.js",
            ),
            cl.parametersList.parameters,
        )
    }

    @Test
    fun `debug mode appends framework-specific timeout zero flag`() {
        val settings = WdioRunSettings(wdioConfigFilePath = "wdio.conf.js")
        val cl = WdioCommandLineBuilder.build(settings, MochaAdapter, wdioMainJs, debug = true)
        assertTrue(cl.parametersList.parameters.containsAll(listOf("--mochaOpts.timeout", "0")))
    }

    @Test
    fun `Jasmine framework label and timeout flag are jasmine-specific`() {
        val settings = WdioRunSettings(
            wdioConfigFilePath = "wdio.conf.js",
            framework = com.perryweather.wdio.framework.Framework.JASMINE,
        )
        val cl = WdioCommandLineBuilder.build(settings, JasmineAdapter, wdioMainJs, debug = true)
        val params = cl.parametersList.parameters
        assertTrue(params.containsAll(listOf("--framework", "jasmine")))
        assertTrue(params.containsAll(listOf("--jasmineOpts.defaultTimeoutInterval", "0")))
    }

    @Test
    fun `node options are parsed and prepended before the wdio main js`() {
        val settings = WdioRunSettings(
            nodeOptions = "--require ts-node/register --max-old-space-size=4096",
            wdioConfigFilePath = "wdio.conf.js",
        )
        val cl = WdioCommandLineBuilder.build(settings, MochaAdapter, wdioMainJs, debug = false)
        val params = cl.parametersList.parameters
        val mainIdx = params.indexOf(wdioMainJs)
        assertTrue(mainIdx > 0)
        assertEquals(
            listOf("--require", "ts-node/register", "--max-old-space-size=4096"),
            params.subList(0, mainIdx),
        )
    }

    @Test
    fun `pre node flags come before user node options`() {
        val settings = WdioRunSettings(
            nodeOptions = "--user-flag",
            wdioConfigFilePath = "wdio.conf.js",
        )
        val cl = WdioCommandLineBuilder.build(
            settings,
            MochaAdapter,
            wdioMainJs,
            debug = false,
            preNodeFlags = listOf("--inspect-brk=12345"),
        )
        val params = cl.parametersList.parameters
        val mainIdx = params.indexOf(wdioMainJs)
        assertEquals(listOf("--inspect-brk=12345", "--user-flag"), params.subList(0, mainIdx))
    }

    @Test
    fun `working directory is applied to the command line`() {
        val settings = WdioRunSettings(workingDir = "/abs/project")
        val cl = WdioCommandLineBuilder.build(settings, MochaAdapter, wdioMainJs, debug = false)
        assertEquals(File("/abs/project"), cl.workDirectory)
    }

    @Test
    fun `env vars from EnvironmentVariablesData propagate to the command line`() {
        val settings = WdioRunSettings(
            envData = EnvironmentVariablesData.create(mapOf("TEST_ENV" to "production"), true),
        )
        val cl = WdioCommandLineBuilder.build(settings, MochaAdapter, wdioMainJs, debug = false)
        assertEquals("production", cl.environment["TEST_ENV"])
    }

    @Test
    fun `blank wdio config file path is omitted from argv`() {
        val settings = WdioRunSettings(wdioConfigFilePath = "")
        val cl = WdioCommandLineBuilder.build(settings, MochaAdapter, wdioMainJs, debug = false)
        val params = cl.parametersList.parameters
        val runIdx = params.indexOf("run")
        assertEquals("--framework", params[runIdx + 1])
    }

    @Test
    fun `blank test file path omits the spec flag pair`() {
        val settings = WdioRunSettings(wdioConfigFilePath = "wdio.conf.js", testFilePath = "")
        val cl = WdioCommandLineBuilder.build(settings, MochaAdapter, wdioMainJs, debug = false)
        assertTrue("--spec" !in cl.parametersList.parameters)
    }
}
