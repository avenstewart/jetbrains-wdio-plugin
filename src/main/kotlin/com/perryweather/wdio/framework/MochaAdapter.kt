// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import com.intellij.javascript.testFramework.JsTestElementPath
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.util.TextRange

object MochaAdapter : JsTestAdapter() {
    override val framework: Framework = Framework.MOCHA
    override val grepFlag: String = "--mochaOpts.grep"
    override val timeoutFlag: String = "--mochaOpts.timeout"

    override fun findTestPath(jsFile: JSFile, range: TextRange): JsTestElementPath? =
        MochaTddFileStructureBuilder.getInstance()
            .fetchCachedTestFileStructure(jsFile)
            .findTestElementPath(range)
}
