// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import com.intellij.javascript.testFramework.AbstractTestFileStructure
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder
import com.intellij.javascript.testing.detection.JsTestFrameworkApiDesign
import com.intellij.javascript.testing.detection.JsTestFrameworkDetector
import com.intellij.lang.javascript.psi.JSFile

class WdioTestFrameworkDetector : JsTestFrameworkDetector {
    override val frameworkName: String = "WebdriverIO"
    override val frameworkApiDesign: JsTestFrameworkApiDesign = JsTestFrameworkApiDesign.GLOBAL_VARIABLES

    override fun findTestsStructure(jsFile: JSFile): AbstractTestFileStructure {
        val jasmine = JasmineFileStructureBuilder.getInstance().fetchCachedTestFileStructure(jsFile)
        if (!jasmine.isEmpty) return jasmine
        return MochaTddFileStructureBuilder.getInstance().fetchCachedTestFileStructure(jsFile)
    }
}
