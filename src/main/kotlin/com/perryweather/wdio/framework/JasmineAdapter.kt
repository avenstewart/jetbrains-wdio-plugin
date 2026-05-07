// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import com.intellij.javascript.testFramework.JsTestElementPath
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.util.TextRange

object JasmineAdapter : JsTestAdapter() {
    override val framework: Framework = Framework.JASMINE
    override val grepFlag: String = "--jasmineOpts.grep"
    override val timeoutFlag: String = "--jasmineOpts.defaultTimeoutInterval"

    override fun findTestPath(jsFile: JSFile, range: TextRange): JsTestElementPath? =
        JasmineFileStructureBuilder.getInstance()
            .fetchCachedTestFileStructure(jsFile)
            .findTestElementPath(range)
}
