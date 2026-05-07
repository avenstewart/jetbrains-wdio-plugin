// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.console

import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.testing.JsTestConsoleProperties
import com.intellij.terminal.TerminalExecutionConsole
import com.perryweather.wdio.runner.WdioRunConfiguration

const val WDIO_FRAMEWORK_NAME: String = "WdioJavaScriptTestRunner"

class WdioConsoleProperties(
    configuration: WdioRunConfiguration,
    executor: Executor,
    private val locator: SMTestLocator,
    private val withTerminalConsole: Boolean,
) : JsTestConsoleProperties(configuration, WDIO_FRAMEWORK_NAME, executor) {

    init {
        isUsePredefinedMessageFilter = false
        setIfUndefined(HIDE_PASSED_TESTS, false)
        setIfUndefined(HIDE_IGNORED_TEST, true)
        setIfUndefined(SCROLL_TO_SOURCE, true)
        setIfUndefined(SELECT_FIRST_DEFECT, true)
        isIdBasedTestTree = true
        isPrintTestingStartedTime = false
    }

    override fun createConsole(): ConsoleView {
        if (withTerminalConsole) {
            return object : TerminalExecutionConsole(project, null) {
                override fun attachToProcess(processHandler: ProcessHandler) {
                    attachToProcess(processHandler, false)
                }
            }
        }
        return super.createConsole()
    }

    override fun getTestLocator(): SMTestLocator = locator

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: com.intellij.execution.testframework.TestConsoleProperties,
    ): OutputToGeneralTestEventsConverter = WdioOutputConverter(testFrameworkName, consoleProperties)

    override fun createRerunFailedTestsAction(consoleView: ConsoleView?): AbstractRerunFailedTestsAction =
        WdioRerunFailedTestAction(consoleView as SMTRunnerConsoleView, this)
}
