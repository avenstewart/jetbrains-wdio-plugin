// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class WdioConfigurationType : ConfigurationType {
    private val factory = WdioConfigurationFactory(this)

    override fun getDisplayName(): String = "WebdriverIO"

    override fun getConfigurationTypeDescription(): String =
        "Run WebdriverIO tests via @wdio/cli"

    override fun getIcon(): Icon = IconLoader.getIcon("/icons/wdio.svg", javaClass)

    override fun getId(): String = "com.perryweather.wdio.WdioConfigurationType"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)

    companion object {
        fun getInstance(): WdioConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(WdioConfigurationType::class.java)
    }
}

class WdioConfigurationFactory(type: WdioConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "com.perryweather.wdio.WdioFactory"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        WdioRunConfiguration(project, this, "WebdriverIO")
}
