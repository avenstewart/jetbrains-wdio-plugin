// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestRunnerBundle
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption
import com.intellij.javascript.nodejs.debug.NodeDebugRunConfiguration
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.testFramework.PreferableRunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.perryweather.wdio.config.WDIO_CLI_PACKAGE_DESCRIPTOR
import com.perryweather.wdio.config.WdioRunSettings
import com.perryweather.wdio.config.WdioRunSettingsSerializer
import com.perryweather.wdio.console.WdioConsoleProperties
import com.perryweather.wdio.console.WdioTestLocationProvider
import com.perryweather.wdio.ui.WdioRunConfigurationEditor
import org.jdom.Element

class WdioRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<WdioRunConfiguration>(project, factory, name),
    JSRunProfileWithCompileBeforeLaunchOption,
    NodeDebugRunConfiguration,
    PreferableRunConfiguration,
    SMRunnerConsolePropertiesProvider {

    var runSettings: WdioRunSettings = WdioRunSettings()

    override val interpreter: NodeJsInterpreter?
        get() = runSettings.interpreterRef.resolve(project)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        WdioRunConfigurationEditor(project)

    override fun readExternal(element: Element) {
        super.readExternal(element)
        runSettings = WdioRunSettingsSerializer.readFromXml(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        WdioRunSettingsSerializer.writeToXml(element, runSettings)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        WdioRunProfileState(
            project = project,
            configuration = this,
            env = environment,
            wdioPackage = getOrResolveWdioPackage(),
            runSettings = runSettings,
        )

    override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties =
        createTestConsoleProperties(executor, withTerminalConsole = true)

    fun createTestConsoleProperties(executor: Executor, withTerminalConsole: Boolean): WdioConsoleProperties =
        WdioConsoleProperties(this, executor, WdioTestLocationProvider(), withTerminalConsole)

    fun getOrResolveWdioPackage(): NodePackage {
        runSettings.wdioPackage?.let { return it }
        val resolved = WDIO_CLI_PACKAGE_DESCRIPTOR.findFirstDirectDependencyPackage(
            project,
            runSettings.interpreterRef.resolve(project),
            getContextFile(),
        )
        runSettings = runSettings.copy(wdioPackage = resolved)
        return resolved
    }

    override fun suggestedName(): String? {
        val path = runSettings.testFilePath
        if (path.isNotBlank()) return path.substringAfterLast('/')
        return TestRunnerBundle.message("all.tests.scope.presentable.text")
    }

    override fun isPreferredOver(other: com.intellij.execution.configurations.RunConfiguration, sourceElement: PsiElement): Boolean =
        runSettings.wdioPackage?.isValid(null, null) == true

    override fun onNewConfigurationCreated() {
        if (runSettings.workingDir.isBlank()) {
            project.basePath?.let { runSettings = runSettings.copy(workingDir = it) }
        }
    }

    private fun getContextFile(): VirtualFile? =
        findFile(runSettings.testFilePath) ?: findFile(runSettings.workingDir)

    private fun findFile(path: String): VirtualFile? =
        if (FileUtil.isAbsolute(path)) LocalFileSystem.getInstance().findFileByPath(path) else null
}
