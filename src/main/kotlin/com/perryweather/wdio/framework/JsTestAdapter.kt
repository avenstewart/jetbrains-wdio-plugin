// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import com.intellij.javascript.testFramework.JsTestElementPath
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.PsiUtilCore

abstract class JsTestAdapter : WdioFrameworkAdapter {
    protected abstract val grepFlag: String
    protected abstract val timeoutFlag: String

    protected open fun findTestPath(jsFile: JSFile, range: TextRange): JsTestElementPath? {
        JasmineFileStructureBuilder.getInstance()
            .fetchCachedTestFileStructure(jsFile)
            .findTestElementPath(range)
            ?.let { return it }
        return MochaTddFileStructureBuilder.getInstance()
            .fetchCachedTestFileStructure(jsFile)
            .findTestElementPath(range)
    }

    override fun matches(element: PsiElement): Boolean {
        if (element is PsiFileSystemItem) return false
        val jsFile = element.containingFile as? JSFile ?: return false
        val range = element.textRange ?: return false
        return findTestPath(jsFile, range) != null
    }

    override fun extractTestTarget(element: PsiElement): TestTarget? {
        if (element is PsiFileSystemItem) return null
        val jsFile = element.containingFile as? JSFile ?: return null
        val virtualFile = PsiUtilCore.getVirtualFile(element) ?: return null
        val range = element.textRange ?: return null
        val path = findTestPath(jsFile, range) ?: return null
        val filter = buildFilter(path)
        return TestTarget(virtualFile.path, filter)
    }

    override fun argvFor(filter: TestFilter, testFilePath: String, debug: Boolean): List<String> = buildList {
        if (filter is TestFilter.Grep) {
            add(grepFlag)
            add(filter.pattern)
        }
        if (debug) {
            add(timeoutFlag)
            add("0")
        }
    }
}

internal fun buildFilter(path: JsTestElementPath): TestFilter {
    val testName = path.testName
    val suites = path.suiteNames
    if (testName == null && suites.isEmpty()) return TestFilter.None
    val pattern = buildGrepPattern(suites, testName)
    return TestFilter.Grep(pattern)
}

internal fun buildGrepPattern(suiteNames: List<String>, testName: String?): String {
    val parts = (suiteNames + listOfNotNull(testName)).map(::escapeRegex)
    val joined = parts.joinToString(" ")
    return if (testName != null) "^$joined$" else "^$joined "
}
