// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.producer

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.perryweather.wdio.runner.WdioConfigurationType
import com.perryweather.wdio.runner.WdioRunConfiguration

class WdioRunConfigurationProducer : LazyRunConfigurationProducer<WdioRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        WdioConfigurationType().configurationFactories.first()

    override fun setupConfigurationFromContext(
        configuration: WdioRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean = false

    override fun isConfigurationFromContext(
        configuration: WdioRunConfiguration,
        context: ConfigurationContext,
    ): Boolean = false
}
