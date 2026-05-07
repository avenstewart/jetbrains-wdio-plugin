// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.config

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.util.io.FileUtil
import com.perryweather.wdio.framework.Framework
import com.perryweather.wdio.framework.TestFilter
import com.perryweather.wdio.framework.escapeRegex
import org.jdom.Element

private const val NODE_INTERPRETER = "node-interpreter"
private const val NODE_OPTIONS = "node-options"
private const val WDIO_PACKAGE = "wdio-package"
private const val WORKING_DIRECTORY = "working-directory"
private const val PASS_PARENT_ENV = "pass-parent-env"
private const val WDIO_CONFIG_FILE_PATH = "wdio-config-file-path"
private const val FRAMEWORK = "wdio-framework"
private const val TEST_FILE = "test-file"

private const val TEST_FILTER = "test-filter"
private const val FILTER_TYPE_ATTR = "type"
private const val FILTER_PATTERN_ATTR = "pattern"
private const val FILTER_EXPRESSION_ATTR = "expression"
private const val FILTER_LINE_ATTR = "line"

private const val LEGACY_TEST_NAMES = "test-names"
private const val LEGACY_TEST_LINE_NUMBERS = "test-line-numbers"
private const val LEGACY_NAME_CHILD = "name"

object WdioRunSettingsSerializer {

    fun readFromXml(parent: Element): WdioRunSettings {
        val interpreterRefName = readTag(parent, NODE_INTERPRETER)?.takeIf { it.isNotEmpty() } ?: "project"
        val interpreterRef = NodeJsInterpreterRef.create(interpreterRefName)
        val nodeOptions = readTag(parent, NODE_OPTIONS).orEmpty()
        val wdioPackage = readTag(parent, WDIO_PACKAGE)?.let { WDIO_CLI_PACKAGE_DESCRIPTOR.createPackage(it) }
        val workingDir = readTag(parent, WORKING_DIRECTORY)?.let(FileUtil::toSystemDependentName).orEmpty()

        val baseEnvData = EnvironmentVariablesData.readExternal(parent)
        val envData = readTag(parent, PASS_PARENT_ENV)?.takeIf { it.isNotEmpty() }
            ?.let { EnvironmentVariablesData.create(baseEnvData.envs, it.toBoolean()) }
            ?: baseEnvData

        val wdioConfigFilePath = readTag(parent, WDIO_CONFIG_FILE_PATH).orEmpty()
        val framework = Framework.fromString(readTag(parent, FRAMEWORK))
        val testFilePath = readTag(parent, TEST_FILE)?.let(FileUtil::toSystemDependentName).orEmpty()

        val testFilter = readTestFilter(parent)
            ?: legacyTestFilterFromTestNames(parent)
            ?: TestFilter.None

        return WdioRunSettings(
            interpreterRef = interpreterRef,
            nodeOptions = nodeOptions,
            wdioPackage = wdioPackage,
            workingDir = workingDir,
            envData = envData,
            wdioConfigFilePath = wdioConfigFilePath,
            framework = framework,
            testFilePath = testFilePath,
            testFilter = testFilter,
        )
    }

    fun writeToXml(parent: Element, settings: WdioRunSettings) {
        writeTag(parent, NODE_INTERPRETER, settings.interpreterRef.referenceName)
        writeTag(parent, NODE_OPTIONS, settings.nodeOptions)
        settings.wdioPackage?.let { writeTag(parent, WDIO_PACKAGE, it.systemIndependentPath) }
        writeTag(parent, WORKING_DIRECTORY, FileUtil.toSystemIndependentName(settings.workingDir))
        settings.envData.writeExternal(parent)
        writeTag(parent, WDIO_CONFIG_FILE_PATH, settings.wdioConfigFilePath)
        writeTag(parent, FRAMEWORK, settings.framework.cliName)
        writeTag(parent, TEST_FILE, FileUtil.toSystemIndependentName(settings.testFilePath))
        writeTestFilter(parent, settings.testFilter)
    }

    private fun readTag(parent: Element, tag: String): String? = parent.getChild(tag)?.text

    private fun writeTag(parent: Element, tag: String, value: String) {
        parent.addContent(Element(tag).apply { text = value })
    }

    private fun readTestFilter(parent: Element): TestFilter? {
        val element = parent.getChild(TEST_FILTER) ?: return null
        return when (element.getAttributeValue(FILTER_TYPE_ATTR)) {
            "grep" -> TestFilter.Grep(element.getAttributeValue(FILTER_PATTERN_ATTR).orEmpty())
            "cucumber-line" -> element.getAttributeValue(FILTER_LINE_ATTR)?.toIntOrNull()
                ?.let(TestFilter::CucumberLine)
                ?: TestFilter.None
            "cucumber-tags" -> TestFilter.CucumberTags(element.getAttributeValue(FILTER_EXPRESSION_ATTR).orEmpty())
            "cucumber-name" -> TestFilter.CucumberName(element.getAttributeValue(FILTER_PATTERN_ATTR).orEmpty())
            "none" -> TestFilter.None
            else -> null
        }
    }

    private fun writeTestFilter(parent: Element, filter: TestFilter) {
        if (filter is TestFilter.None) return
        val element = Element(TEST_FILTER)
        when (filter) {
            is TestFilter.None -> Unit
            is TestFilter.Grep -> {
                element.setAttribute(FILTER_TYPE_ATTR, "grep")
                element.setAttribute(FILTER_PATTERN_ATTR, filter.pattern)
            }
            is TestFilter.CucumberLine -> {
                element.setAttribute(FILTER_TYPE_ATTR, "cucumber-line")
                element.setAttribute(FILTER_LINE_ATTR, filter.line.toString())
            }
            is TestFilter.CucumberTags -> {
                element.setAttribute(FILTER_TYPE_ATTR, "cucumber-tags")
                element.setAttribute(FILTER_EXPRESSION_ATTR, filter.expression)
            }
            is TestFilter.CucumberName -> {
                element.setAttribute(FILTER_TYPE_ATTR, "cucumber-name")
                element.setAttribute(FILTER_PATTERN_ATTR, filter.pattern)
            }
        }
        parent.addContent(element)
    }

    private fun legacyTestFilterFromTestNames(parent: Element): TestFilter? {
        val element = parent.getChild(LEGACY_TEST_NAMES) ?: return null
        val names = JDOMExternalizerUtil.getChildrenValueAttributes(element, LEGACY_NAME_CHILD)
        if (names.isEmpty()) return null
        val joined = names.joinToString(" ", transform = ::escapeRegex)
        return TestFilter.Grep("^$joined$")
    }
}
