// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.GenericProgramRunner

class WdioRunProgramRunner : GenericProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String = "com.perryweather.wdio.WdioRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        profile is WdioRunConfiguration
}
