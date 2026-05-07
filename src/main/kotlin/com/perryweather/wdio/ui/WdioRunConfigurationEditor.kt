// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.ui

import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.util.NodePackageField
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.perryweather.wdio.config.WDIO_CLI_PACKAGE_DESCRIPTOR
import com.perryweather.wdio.config.WDIO_CLI_PACKAGE_NAME
import com.perryweather.wdio.framework.Framework
import com.perryweather.wdio.runner.WdioRunConfiguration
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList

class WdioRunConfigurationEditor(project: Project) : SettingsEditor<WdioRunConfiguration>() {

    private val interpreterField = NodeJsInterpreterField(project, false)
    private val nodeOptionsField = RawCommandLineEditor()
    private val wdioPackageField = NodePackageField(interpreterField, WDIO_CLI_PACKAGE_NAME)
    private val workingDirField = JBTextField()
    private val envVarsField = EnvironmentVariablesTextFieldWithBrowseButton()
    private val wdioConfigFileField = JBTextField()
    private val testFilePathField = JBTextField()
    private val frameworkField = JComboBox(Framework.entries.toTypedArray()).apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = (value as? Framework)?.displayName ?: ""
                return this
            }
        }
    }

    private val panel: JComponent = FormBuilder.createFormBuilder()
        .addLabeledComponent("Node interpreter:", interpreterField)
        .addLabeledComponent("Node options:", nodeOptionsField)
        .addLabeledComponent("WebdriverIO package:", wdioPackageField)
        .addLabeledComponent("Working directory:", workingDirField)
        .addLabeledComponent("Environment variables:", envVarsField)
        .addLabeledComponent("WDIO config file:", wdioConfigFileField)
        .addLabeledComponent("Framework:", frameworkField)
        .addLabeledComponent("Test file:", testFilePathField)
        .panel

    override fun createEditor(): JComponent = panel

    override fun resetEditorFrom(s: WdioRunConfiguration) {
        val rs = s.runSettings
        interpreterField.interpreterRef = rs.interpreterRef
        nodeOptionsField.text = rs.nodeOptions
        rs.wdioPackage?.let { wdioPackageField.selected = it }
        workingDirField.text = rs.workingDir
        envVarsField.data = rs.envData
        wdioConfigFileField.text = rs.wdioConfigFilePath
        frameworkField.selectedItem = rs.framework
        testFilePathField.text = rs.testFilePath
    }

    override fun applyEditorTo(s: WdioRunConfiguration) {
        s.runSettings = s.runSettings.copy(
            interpreterRef = interpreterField.interpreterRef,
            nodeOptions = nodeOptionsField.text,
            wdioPackage = wdioPackageField.selected,
            workingDir = workingDirField.text,
            envData = envVarsField.data,
            wdioConfigFilePath = wdioConfigFileField.text,
            framework = frameworkField.selectedItem as? Framework ?: Framework.MOCHA,
            testFilePath = testFilePathField.text,
        )
    }
}
