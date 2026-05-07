// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.config

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageDescriptor
import com.perryweather.wdio.framework.Framework
import com.perryweather.wdio.framework.TestFilter

const val WDIO_CLI_PACKAGE_NAME: String = "@wdio/cli"
val WDIO_CLI_PACKAGE_DESCRIPTOR: NodePackageDescriptor = NodePackageDescriptor(WDIO_CLI_PACKAGE_NAME)

data class WdioRunSettings(
    val interpreterRef: NodeJsInterpreterRef = NodeJsInterpreterRef.create(""),
    val nodeOptions: String = "",
    val wdioPackage: NodePackage? = null,
    val workingDir: String = "",
    val envData: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT,
    val wdioConfigFilePath: String = "",
    val framework: Framework = Framework.MOCHA,
    val testFilePath: String = "",
    val testFilter: TestFilter = TestFilter.None,
)