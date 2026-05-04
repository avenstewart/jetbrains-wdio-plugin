// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.ui

import com.intellij.openapi.options.SettingsEditor
import com.perryweather.wdio.runner.WdioRunConfiguration
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class WdioRunConfigurationEditor : SettingsEditor<WdioRunConfiguration>() {
    private val panel = JPanel().apply {
        add(JLabel("WebdriverIO settings will be available in the next release."))
    }

    override fun resetEditorFrom(s: WdioRunConfiguration) = Unit

    override fun applyEditorTo(s: WdioRunConfiguration) = Unit

    override fun createEditor(): JComponent = panel
}
