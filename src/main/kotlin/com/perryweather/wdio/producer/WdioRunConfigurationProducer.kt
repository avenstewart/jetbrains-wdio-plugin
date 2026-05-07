// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.producer

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.javascript.testing.JsTestRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.perryweather.wdio.config.WDIO_CLI_PACKAGE_DESCRIPTOR
import com.perryweather.wdio.config.WdioConfigDiscovery
import com.perryweather.wdio.framework.MochaAdapter
import com.perryweather.wdio.framework.WdioFrameworkAdapter
import com.perryweather.wdio.runner.WdioConfigurationType
import com.perryweather.wdio.runner.WdioRunConfiguration
import java.nio.file.Path

class WdioRunConfigurationProducer : JsTestRunConfigurationProducer<WdioRunConfiguration>(
    WDIO_CLI_PACKAGE_DESCRIPTOR,
    emptyList(),
) {
    override fun getConfigurationFactory(): ConfigurationFactory =
        WdioConfigurationType.getInstance().configurationFactories.first()

    override fun setupConfigurationFromCompatibleContext(
        configuration: WdioRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val element = context.psiLocation ?: return false
        if (!isActiveFor(element, context)) return false

        val adapter = adapterFor(configuration)
        val testTarget = adapter.extractTestTarget(element) ?: return false
        val virtualFile = PsiUtilCore.getVirtualFile(element) ?: return false

        var settings = configuration.runSettings.copy(
            testFilePath = FileUtil.toSystemDependentName(testTarget.filePath),
            testFilter = testTarget.filter,
        )

        if (settings.workingDir.isBlank()) {
            val guessedWorkingDir = guessWorkingDirectory(element.project, virtualFile.path)?.path
                ?: configuration.project.basePath
            if (guessedWorkingDir != null) {
                settings = settings.copy(workingDir = FileUtil.toSystemDependentName(guessedWorkingDir))
            }
        }

        if (settings.wdioConfigFilePath.isBlank()) {
            discoverFirstConfig(settings.workingDir)?.let {
                settings = settings.copy(wdioConfigFilePath = FileUtil.toSystemDependentName(it))
            }
        }

        configuration.runSettings = settings
        sourceElement.set(element)
        configuration.setGeneratedName()
        return true
    }

    override fun isConfigurationFromCompatibleContext(
        configuration: WdioRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val element = context.psiLocation ?: return false
        val adapter = adapterFor(configuration)
        val target = adapter.extractTestTarget(element) ?: return false
        val settings = configuration.runSettings
        return FileUtil.toSystemDependentName(target.filePath) == settings.testFilePath &&
            target.filter == settings.testFilter
    }

    private fun adapterFor(configuration: WdioRunConfiguration): WdioFrameworkAdapter =
        WdioFrameworkAdapter.forFramework(configuration.runSettings.framework) ?: MochaAdapter

    private fun isActiveFor(element: PsiElement, context: ConfigurationContext): Boolean {
        if (PsiUtilCore.getVirtualFile(element) == null) return false
        return isTestRunnerPackageAvailableFor(element, context)
    }

    private fun discoverFirstConfig(workingDir: String): String? {
        if (workingDir.isBlank()) return null
        val root = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(workingDir))
            ?: return null
        val rootPath: Path = try {
            root.toNioPath()
        } catch (_: UnsupportedOperationException) {
            return null
        }
        return WdioConfigDiscovery.discover(rootPath).firstOrNull()?.toString()
    }
}
