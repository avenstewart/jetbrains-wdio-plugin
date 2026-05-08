// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.config

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.util.JDOMUtil
import com.perryweather.wdio.framework.Framework
import com.perryweather.wdio.framework.TestFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WdioRunSettingsSerializerTest {

    @Test
    fun `round-trip preserves all populated fields with grep filter`() {
        val original = WdioRunSettings(
            nodeOptions = "--require ts-node/register",
            workingDir = "/abs/project",
            envData = EnvironmentVariablesData.create(mapOf("FOO" to "bar"), false),
            wdioConfigFilePath = "wdio.web.conf.ts",
            framework = Framework.JASMINE,
            testFilePath = "/abs/test.spec.ts",
            testFilter = TestFilter.Grep("^Foo bar$"),
        )

        val element = roundTrip(original)
        val restored = WdioRunSettingsSerializer.readFromXml(element)

        assertEquals(original.nodeOptions, restored.nodeOptions)
        assertEquals(original.workingDir, restored.workingDir)
        assertEquals(original.envData, restored.envData)
        assertEquals(original.wdioConfigFilePath, restored.wdioConfigFilePath)
        assertEquals(original.framework, restored.framework)
        assertEquals(original.testFilePath, restored.testFilePath)
        assertEquals(original.testFilter, restored.testFilter)
    }

    @Test
    fun `cucumber name filter round-trips`() {
        val original = WdioRunSettings(testFilter = TestFilter.CucumberName("^my scenario$"))
        val restored = WdioRunSettingsSerializer.readFromXml(roundTrip(original))
        assertEquals(TestFilter.CucumberName("^my scenario$"), restored.testFilter)
    }

    @Test
    fun `cucumber tags filter round-trips`() {
        val original = WdioRunSettings(testFilter = TestFilter.CucumberTags("@smoke and not @wip"))
        val restored = WdioRunSettingsSerializer.readFromXml(roundTrip(original))
        assertEquals(TestFilter.CucumberTags("@smoke and not @wip"), restored.testFilter)
    }

    @Test
    fun `None filter writes no test-filter element`() {
        val element = roundTrip(WdioRunSettings(testFilter = TestFilter.None))
        assertTrue(element.getChild("test-filter") == null)
        val restored = WdioRunSettingsSerializer.readFromXml(element)
        assertEquals(TestFilter.None, restored.testFilter)
    }

    @Test
    fun `framework written in lowercase cliName`() {
        val element = roundTrip(WdioRunSettings(framework = Framework.MOCHA))
        assertEquals("mocha", element.getChild("wdio-framework")?.text)
    }

    @Test
    fun `legacy XML with display-case framework reads correctly`() {
        val xml = """
            <config>
              <wdio-framework>Mocha</wdio-framework>
            </config>
        """.trimIndent()
        val settings = WdioRunSettingsSerializer.readFromXml(JDOMUtil.load(xml))
        assertEquals(Framework.MOCHA, settings.framework)
    }

    @Test
    fun `legacy test-names map to grep filter with anchored regex`() {
        val xml = """
            <config>
              <wdio-framework>Mocha</wdio-framework>
              <test-file>/abs/foo.spec.js</test-file>
              <test-names>
                <name value="Outer"/>
                <name value="Inner"/>
                <name value="does the thing"/>
              </test-names>
              <test-line-numbers>
                <name value="42"/>
              </test-line-numbers>
            </config>
        """.trimIndent()
        val settings = WdioRunSettingsSerializer.readFromXml(JDOMUtil.load(xml))
        assertEquals(TestFilter.Grep("^Outer Inner does the thing$"), settings.testFilter)
    }

    @Test
    fun `legacy test-names with regex metacharacters are escaped`() {
        val xml = """
            <config>
              <test-names>
                <name value="needs.escape*"/>
                <name value="(parens)"/>
              </test-names>
            </config>
        """.trimIndent()
        val settings = WdioRunSettingsSerializer.readFromXml(JDOMUtil.load(xml))
        assertEquals(TestFilter.Grep("^needs\\.escape\\* \\(parens\\)$"), settings.testFilter)
    }

    @Test
    fun `legacy test-line-numbers alone do not produce a filter`() {
        val xml = """
            <config>
              <test-line-numbers>
                <name value="42"/>
              </test-line-numbers>
            </config>
        """.trimIndent()
        val settings = WdioRunSettingsSerializer.readFromXml(JDOMUtil.load(xml))
        assertEquals(TestFilter.None, settings.testFilter)
    }

    @Test
    fun `legacy pass-parent-env tag is honored when present`() {
        val xml = """
            <config>
              <pass-parent-env>false</pass-parent-env>
            </config>
        """.trimIndent()
        val settings = WdioRunSettingsSerializer.readFromXml(JDOMUtil.load(xml))
        assertTrue(!settings.envData.isPassParentEnvs)
    }

    @Test
    fun `empty XML yields default settings`() {
        val settings = WdioRunSettingsSerializer.readFromXml(JDOMUtil.load("<config/>"))
        assertEquals("", settings.nodeOptions)
        assertEquals("", settings.workingDir)
        assertEquals("", settings.wdioConfigFilePath)
        assertEquals("", settings.testFilePath)
        assertEquals(Framework.MOCHA, settings.framework)
        assertEquals(TestFilter.None, settings.testFilter)
        assertNotNull(settings.envData)
    }

    private fun roundTrip(settings: WdioRunSettings): org.jdom.Element {
        val element = org.jdom.Element("config")
        WdioRunSettingsSerializer.writeToXml(element, settings)
        return element
    }
}
