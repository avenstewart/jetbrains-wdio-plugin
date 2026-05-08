// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter
import com.intellij.javascript.nodejs.NodeStackTraceFilter
import com.intellij.javascript.nodejs.debug.NodeLocalDebuggableRunProfileStateSync
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.project.Project
import com.perryweather.wdio.config.WdioRunSettings
import com.perryweather.wdio.console.WdioConsoleProperties
import com.perryweather.wdio.framework.MochaAdapter
import com.perryweather.wdio.framework.WdioFrameworkAdapter
import java.io.File

class WdioRunProfileState(
    private val project: Project,
    private val configuration: WdioRunConfiguration,
    private val env: ExecutionEnvironment,
    private val wdioPackage: NodePackage,
    val runSettings: WdioRunSettings,
) : NodeLocalDebuggableRunProfileStateSync() {

    var failedTests: List<List<String>>? = null

    @Throws(ExecutionException::class)
    override fun executeSync(configurator: CommandLineDebugConfigurator?): ExecutionResult {
        val interpreter = runSettings.interpreterRef.resolveNotNull(project)
        val commandLine = NodeCommandLineUtil.createCommandLineForTestTools()
        NodeCommandLineUtil.configureCommandLine(commandLine, configurator, interpreter) { debugMode ->
            applyWdioInvocation(commandLine, interpreter, debugMode)
        }

        val processHandler = NodeCommandLineUtil.createProcessHandler(commandLine, false)
        val consoleProperties = configuration.createTestConsoleProperties(
            env.executor,
            NodeCommandLineUtil.shouldUseTerminalConsole(processHandler),
        )
        val consoleView = createSMTRunnerConsoleView(commandLine.workDirectory, consoleProperties)
        ProcessTerminatedListener.attach(processHandler)
        consoleView.attachToProcess(processHandler)

        val result = DefaultExecutionResult(consoleView, processHandler)
        result.setRestartActions(consoleProperties.createRerunFailedTestsAction(consoleView))
        return result
    }

    private fun applyWdioInvocation(commandLine: GeneralCommandLine, interpreter: NodeJsInterpreter, debug: Boolean) {
        val preNodeFlags = ArrayList(commandLine.parametersList.parameters)
        NodeCommandLineUtil.configureUsefulEnvironment(commandLine)
        NodeCommandLineUtil.prependNodeDirToPATH(commandLine, interpreter)

        val wdioCliMainJs = wdioPackage.findBinFilePath("wdio", null)?.toAbsolutePath()?.toString()
            ?: throw ExecutionException("Cannot resolve @wdio/cli bin file from ${wdioPackage.systemDependentPath}")
        val adapter = WdioFrameworkAdapter.forFramework(runSettings.framework) ?: MochaAdapter

        WdioCommandLineBuilder.apply(
            commandLine = commandLine,
            settings = runSettings,
            adapter = adapter,
            wdioCliMainJs = wdioCliMainJs,
            debug = debug,
            preNodeFlags = preNodeFlags,
            intellijReporterPath = WdioReporterExtractor.extractedReporterPath().toString(),
        )
    }

    private fun createSMTRunnerConsoleView(workDir: File, properties: WdioConsoleProperties): ConsoleView {
        val baseConsole = SMTestRunnerConnectionUtil.createConsole(
            properties.testFrameworkName,
            properties as TestConsoleProperties,
        )
        properties.addStackTraceFilter(NodeStackTraceFilter(project, workDir))
        properties.stackTrackFilters.forEach { baseConsole.addMessageFilter(it) }
        baseConsole.addMessageFilter(NodeConsoleAdditionalFilter(project, workDir))
        return baseConsole
    }
}
