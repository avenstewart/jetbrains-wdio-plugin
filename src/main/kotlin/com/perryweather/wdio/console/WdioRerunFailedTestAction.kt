// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.console

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.javascript.testFramework.util.EscapeUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.perryweather.wdio.runner.WdioRunConfiguration
import com.perryweather.wdio.runner.WdioRunProfileState

class WdioRerunFailedTestAction(
    consoleView: SMTRunnerConsoleView,
    consoleProperties: WdioConsoleProperties,
) : AbstractRerunFailedTestsAction(consoleView) {

    init {
        init(consoleProperties)
        model = consoleView.resultsViewer
    }

    override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile {
        val configuration = myConsoleProperties.configuration as WdioRunConfiguration
        val state = WdioRunProfileState(
            configuration.project,
            configuration,
            environment,
            configuration.getOrResolveWdioPackage(),
            configuration.runSettings,
        )
        state.failedTests = collectFailedTestFqns(configuration.project)
        return object : MyRunProfile(configuration as RunConfigurationBase<*>) {
            override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = state
        }
    }

    private fun collectFailedTestFqns(project: Project): List<List<String>> =
        getFailedTests(project).mapNotNull(::convertToFqn)

    private fun convertToFqn(test: AbstractTestProxy): List<String>? {
        val url = test.locationUrl ?: return null
        if (!test.isLeaf) return null
        val fqn = EscapeUtils.split(VirtualFileManager.extractPath(url), '.')
        return fqn.takeIf { it.isNotEmpty() }
    }
}
