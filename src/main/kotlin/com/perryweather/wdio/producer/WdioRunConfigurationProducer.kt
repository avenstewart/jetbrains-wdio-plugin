// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.producer

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.javascript.testFramework.PreferableRunConfiguration
import com.intellij.javascript.testing.JsPackageDependentTestRunConfigurationProducer
import com.intellij.javascript.testing.detection.JsTestFrameworkDetector
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.jetbrains.nodejs.mocha.execution.MochaRunConfiguration
import com.perryweather.wdio.config.WDIO_CLI_PACKAGE_DESCRIPTOR
import com.perryweather.wdio.config.WdioConfigDiscovery
import com.perryweather.wdio.framework.CucumberAdapter
import com.perryweather.wdio.framework.Framework
import com.perryweather.wdio.framework.MochaAdapter
import com.perryweather.wdio.framework.WdioFrameworkAdapter
import com.perryweather.wdio.framework.WdioTestFrameworkDetector
import com.perryweather.wdio.runner.WdioConfigurationType
import com.perryweather.wdio.runner.WdioRunConfiguration
import java.nio.file.Path

class WdioRunConfigurationProducer : JsPackageDependentTestRunConfigurationProducer<WdioRunConfiguration>(
    WDIO_CLI_PACKAGE_DESCRIPTOR,
    emptyList(),
) {
    private val frameworkDetector = WdioTestFrameworkDetector()

    override fun getConfigurationFactory(): ConfigurationFactory =
        WdioConfigurationType.getInstance().configurationFactories.first()

    override fun getTestFrameworkDetector(): JsTestFrameworkDetector = frameworkDetector

    override fun setupConfigurationFromCompatibleContext(
        configuration: WdioRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val element = context.psiLocation ?: return false
        if (!isActiveFor(element, context)) return false

        val adapter = adapterFor(configuration, element)
        val testTarget = adapter.extractTestTarget(element) ?: return false
        val virtualFile = PsiUtilCore.getVirtualFile(element) ?: return false

        var settings = configuration.runSettings.copy(
            testFilePath = FileUtil.toSystemDependentName(testTarget.filePath),
            testFilter = testTarget.filter,
            framework = adapter.framework,
        )

        if (settings.workingDir.isBlank()) {
            val guessedWorkingDir = guessWorkingDirectory(element.project, virtualFile.path)?.path
                ?: configuration.project.basePath
            if (guessedWorkingDir != null) {
                settings = settings.copy(workingDir = FileUtil.toSystemDependentName(guessedWorkingDir))
            }
        }

        if (settings.wdioConfigFilePath.isBlank()) {
            discoverFirstConfig(settings.workingDir)?.let { discoveredPath ->
                val detected = WdioConfigDiscovery.detectFramework(discoveredPath)
                settings = settings.copy(
                    wdioConfigFilePath = FileUtil.toSystemDependentName(discoveredPath.toString()),
                    framework = detected ?: settings.framework,
                )
            }
        }

        if (settings.wdioPackage == null) {
            val pkg = WDIO_CLI_PACKAGE_DESCRIPTOR.findFirstDirectDependencyPackage(
                configuration.project,
                settings.interpreterRef.resolve(configuration.project),
                virtualFile,
            )
            if (!pkg.isEmptyPath) {
                settings = settings.copy(wdioPackage = pkg)
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
        val adapter = adapterFor(configuration, element)
        val target = adapter.extractTestTarget(element) ?: return false
        val settings = configuration.runSettings
        return FileUtil.toSystemDependentName(target.filePath) == settings.testFilePath &&
            target.filter == settings.testFilter
    }

    override fun isPreferredConfiguration(self: ConfigurationFromContext, other: ConfigurationFromContext?): Boolean {
        if (other == null) return true
        val otherConfig = other.configuration
        if (otherConfig is MochaRunConfiguration) return true
        val preferable = otherConfig as? PreferableRunConfiguration ?: return true
        return !preferable.isPreferredOver(self.configuration, self.sourceElement)
    }

    private fun adapterFor(configuration: WdioRunConfiguration, element: PsiElement): WdioFrameworkAdapter {
        // Gherkin clicks always go to the Cucumber adapter regardless of the configured
        // framework, so we don't try to send a .feature file through Mocha or Jasmine.
        if (CucumberAdapter.matches(element)) return CucumberAdapter
        return WdioFrameworkAdapter.forFramework(configuration.runSettings.framework)
            ?.takeIf { it.framework != Framework.CUCUMBER }
            ?: MochaAdapter
    }

    private fun isActiveFor(element: PsiElement, context: ConfigurationContext): Boolean {
        val file = PsiUtilCore.getVirtualFile(element) ?: return false
        if (isTestRunnerAvailableFor(element, context)) return true
        return hasWdioCliInAncestorNodeModules(file)
    }

    private fun hasWdioCliInAncestorNodeModules(file: VirtualFile): Boolean {
        var dir: VirtualFile? = if (file.isDirectory) file else file.parent
        while (dir != null) {
            val pkg = dir.findFileByRelativePath("node_modules/@wdio/cli/package.json")
            if (pkg != null && pkg.exists() && !pkg.isDirectory) return true
            dir = dir.parent
        }
        return false
    }

    private fun discoverFirstConfig(workingDir: String): Path? {
        if (workingDir.isBlank()) return null
        val root = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(workingDir))
            ?: return null
        val rootPath: Path = try {
            root.toNioPath()
        } catch (_: UnsupportedOperationException) {
            return null
        }
        return WdioConfigDiscovery.discover(rootPath).firstOrNull()
    }
}
