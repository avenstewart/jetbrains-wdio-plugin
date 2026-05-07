// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.console

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.javascript.testFramework.JsTestFileByTestNameIndex
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder
import com.intellij.javascript.testFramework.util.EscapeUtils
import com.intellij.javascript.testFramework.util.JsTestFqn
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSTestFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope

private const val PROTOCOL = "test"
private const val SPLIT_CHAR = '.'

class WdioTestLocationProvider : SMTestLocator {

    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope,
    ): List<Location<PsiElement>> = throw IllegalStateException("Use the metaInfo-aware overload")

    override fun getLocation(
        protocol: String,
        path: String,
        metaInfo: String?,
        project: Project,
        scope: GlobalSearchScope,
    ): List<Location<PsiElement>> {
        if (protocol != PROTOCOL) return emptyList()
        val fqn = EscapeUtils.split(path, SPLIT_CHAR)
        if (fqn.isEmpty()) return emptyList()
        val element = findJasmineElement(project, fqn, metaInfo)
            ?: findTddElement(project, fqn, metaInfo)
            ?: return emptyList()
        return listOfNotNull(PsiLocation.fromPsiElement(element))
    }

    private fun findJasmineElement(project: Project, fqn: List<String>, testFilePath: String?): PsiElement? {
        val anchor = findFile(testFilePath) ?: return null
        val testFqn = JsTestFqn(JSTestFileType.JASMINE, fqn)
        val scope = GlobalSearchScope.projectScope(project)
        for (vf in JsTestFileByTestNameIndex.findFiles(testFqn, scope, anchor)) {
            val jsFile = PsiManager.getInstance(project).findFile(vf) as? JSFile ?: continue
            val element = JasmineFileStructureBuilder.getInstance()
                .fetchCachedTestFileStructure(jsFile)
                .findPsiElement(testFqn.names, null)
            if (element != null && element.isValid) return element
        }
        return null
    }

    private fun findTddElement(project: Project, fqn: List<String>, testFilePath: String?): PsiElement? {
        val anchor = findFile(testFilePath) ?: return null
        val testFqn = JsTestFqn(JSTestFileType.TDD, fqn)
        val scope = GlobalSearchScope.projectScope(project)
        for (vf in JsTestFileByTestNameIndex.findFiles(testFqn, scope, anchor)) {
            val jsFile = PsiManager.getInstance(project).findFile(vf) as? JSFile ?: continue
            val suiteNames = fqn.subList(0, fqn.size - 1)
            val testName = fqn.last()
            val element = MochaTddFileStructureBuilder.getInstance()
                .fetchCachedTestFileStructure(jsFile)
                .findPsiElement(suiteNames, testName)
            if (element != null && element.isValid) return element
        }
        return null
    }

    private fun findFile(testFilePath: String?): VirtualFile? {
        if (testFilePath.isNullOrBlank()) return null
        return LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(testFilePath))
    }
}
