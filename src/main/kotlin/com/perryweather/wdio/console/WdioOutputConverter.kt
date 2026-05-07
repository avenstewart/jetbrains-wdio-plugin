// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.console

import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.openapi.util.Key

class WdioOutputConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties,
) : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    override fun processConsistentText(text: String, outputType: Key<*>) {
        super.processConsistentText(stripWorkerPrefix(text), outputType)
    }

    companion object {
        private val WORKER_PREFIX = Regex("""^\[\d+-\d+] """)

        fun stripWorkerPrefix(text: String): String {
            val match = WORKER_PREFIX.find(text) ?: return text
            val rest = text.substring(match.range.last + 1)
            return if (rest.isBlank() || rest.startsWith("##teamcity[")) rest else text
        }
    }
}
