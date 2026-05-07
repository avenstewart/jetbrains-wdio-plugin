// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.plugins.cucumber.psi.GherkinFeature
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.psi.GherkinScenario
import org.jetbrains.plugins.cucumber.psi.GherkinScenarioOutline
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder
import org.jetbrains.plugins.cucumber.psi.GherkinTag

object CucumberAdapter : WdioFrameworkAdapter {
    override val framework: Framework = Framework.CUCUMBER

    override fun matches(element: PsiElement): Boolean {
        if (element is PsiFileSystemItem && element !is GherkinFile) return false
        return element.containingFile is GherkinFile
    }

    override fun extractTestTarget(element: PsiElement): TestTarget? {
        if (element is PsiFileSystemItem && element !is GherkinFile) return null
        val gherkinFile = element.containingFile as? GherkinFile ?: return null
        val virtualFile = PsiUtilCore.getVirtualFile(element) ?: return null

        val tag = PsiTreeUtil.getParentOfType(element, GherkinTag::class.java, false)
        if (tag != null) {
            return TestTarget(virtualFile.path, TestFilter.CucumberTags(tag.text.trim()))
        }

        val scenario = PsiTreeUtil.getParentOfType(
            element,
            GherkinScenario::class.java,
            GherkinScenarioOutline::class.java,
        )
        if (scenario is GherkinStepsHolder) {
            val line = lineNumberFor(scenario)
            return TestTarget(virtualFile.path, TestFilter.CucumberLine(line))
        }

        if (PsiTreeUtil.getParentOfType(element, GherkinFeature::class.java, false) != null ||
            element is GherkinFile
        ) {
            return TestTarget(virtualFile.path, TestFilter.None)
        }
        return null
    }

    override fun argvFor(filter: TestFilter, testFilePath: String, debug: Boolean): List<String> = buildList {
        when (filter) {
            is TestFilter.CucumberLine -> {
                if (testFilePath.isNotBlank()) {
                    add("--cucumberFeaturesWithLineNumbers")
                    add("$testFilePath:${filter.line}")
                }
            }
            is TestFilter.CucumberTags -> {
                add("--cucumberOpts.tags")
                add(filter.expression)
            }
            is TestFilter.CucumberName -> {
                add("--cucumberOpts.name")
                add(filter.pattern)
            }
            else -> Unit
        }
        if (debug) {
            add("--cucumberOpts.timeout")
            add("0")
        }
    }

    private fun lineNumberFor(element: PsiElement): Int {
        val document: Document = PsiDocumentManager.getInstance(element.project)
            .getDocument(element.containingFile)
            ?: return 1
        return document.getLineNumber(element.textRange.startOffset) + 1
    }
}
