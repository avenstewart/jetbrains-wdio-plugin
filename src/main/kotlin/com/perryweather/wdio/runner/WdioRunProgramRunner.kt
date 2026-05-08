// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager

class WdioRunProgramRunner : GenericProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String = "com.perryweather.wdio.WdioRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        profile is WdioRunConfiguration &&
            (executorId == DefaultRunExecutor.EXECUTOR_ID || executorId == DefaultDebugExecutor.EXECUTOR_ID)

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        FileDocumentManager.getInstance().saveAllDocuments()
        val executionResult = state.execute(environment.executor, this) ?: return null
        return RunContentBuilder(executionResult, environment).showRunContent(environment.contentToReuse)
    }
}
