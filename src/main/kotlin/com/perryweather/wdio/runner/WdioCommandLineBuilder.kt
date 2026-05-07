// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.execution.ParametersListUtil
import com.perryweather.wdio.config.WdioRunSettings
import com.perryweather.wdio.framework.WdioFrameworkAdapter
import java.nio.charset.StandardCharsets

object WdioCommandLineBuilder {

    fun build(
        settings: WdioRunSettings,
        adapter: WdioFrameworkAdapter,
        wdioCliMainJs: String,
        debug: Boolean,
        preNodeFlags: List<String> = emptyList(),
        intellijReporterPath: String? = null,
    ): GeneralCommandLine = GeneralCommandLine().also {
        apply(it, settings, adapter, wdioCliMainJs, debug, preNodeFlags, intellijReporterPath)
    }

    fun apply(
        commandLine: GeneralCommandLine,
        settings: WdioRunSettings,
        adapter: WdioFrameworkAdapter,
        wdioCliMainJs: String,
        debug: Boolean,
        preNodeFlags: List<String> = emptyList(),
        intellijReporterPath: String? = null,
    ) {
        commandLine.charset = StandardCharsets.UTF_8
        if (settings.workingDir.isNotBlank()) {
            commandLine.withWorkDirectory(settings.workingDir)
        }
        settings.envData.configureCommandLine(commandLine, true)

        commandLine.parametersList.clearAll()
        commandLine.parametersList.addAll(preNodeFlags)
        if (settings.nodeOptions.isNotBlank()) {
            commandLine.parametersList.addAll(ParametersListUtil.parse(settings.nodeOptions.trim()))
        }
        commandLine.parametersList.add(wdioCliMainJs)

        commandLine.parametersList.add("run")
        if (settings.wdioConfigFilePath.isNotBlank()) {
            commandLine.parametersList.add(settings.wdioConfigFilePath)
        }
        commandLine.parametersList.add("--framework")
        commandLine.parametersList.add(adapter.framework.cliName)
        commandLine.parametersList.addAll(adapter.argvFor(settings.testFilter, settings.testFilePath, debug))
        if (intellijReporterPath != null) {
            commandLine.parametersList.add("--reporters")
            commandLine.parametersList.add(intellijReporterPath)
        }
        if (settings.testFilePath.isNotBlank()) {
            commandLine.parametersList.add("--spec")
            commandLine.parametersList.add(settings.testFilePath)
        }
    }
}
