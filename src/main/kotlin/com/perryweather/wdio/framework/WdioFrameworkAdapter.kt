// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import com.intellij.psi.PsiElement

sealed interface WdioFrameworkAdapter {
    val framework: Framework

    fun matches(element: PsiElement): Boolean

    fun extractTestTarget(element: PsiElement): TestTarget?

    fun argvFor(filter: TestFilter, testFilePath: String, debug: Boolean): List<String>

    companion object {
        val ALL: List<WdioFrameworkAdapter> = listOf(MochaAdapter, JasmineAdapter, CucumberAdapter)

        fun forFramework(framework: Framework): WdioFrameworkAdapter? =
            ALL.firstOrNull { it.framework == framework }
    }
}

data class TestTarget(
    val filePath: String,
    val filter: TestFilter,
)
