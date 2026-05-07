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
    ): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        commandLine.charset = StandardCharsets.UTF_8
        if (settings.workingDir.isNotBlank()) {
            commandLine.withWorkDirectory(settings.workingDir)
        }
        settings.envData.configureCommandLine(commandLine, true)

        commandLine.addParameters(preNodeFlags)
        if (settings.nodeOptions.isNotBlank()) {
            commandLine.addParameters(ParametersListUtil.parse(settings.nodeOptions.trim()))
        }
        commandLine.addParameter(wdioCliMainJs)

        commandLine.addParameter("run")
        if (settings.wdioConfigFilePath.isNotBlank()) {
            commandLine.addParameter(settings.wdioConfigFilePath)
        }
        commandLine.addParameter("--framework")
        commandLine.addParameter(adapter.framework.cliName)
        commandLine.parametersList.addAll(adapter.argvFor(settings.testFilter, debug))
        if (settings.testFilePath.isNotBlank()) {
            commandLine.addParameter("--spec")
            commandLine.addParameter(settings.testFilePath)
        }
        return commandLine
    }
}
