// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import com.intellij.javascript.testing.detection.JsTestFrameworkApiDesign
import com.intellij.javascript.testing.detection.JsTestFrameworkDetector

class WdioTestFrameworkDetector : JsTestFrameworkDetector {
    override val frameworkName: String = "WebdriverIO"
    override val frameworkApiDesign: JsTestFrameworkApiDesign = JsTestFrameworkApiDesign.GLOBAL_VARIABLES
}
