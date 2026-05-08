// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.console

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope

private const val PROTOCOL = "wdio"
private const val PATH_SEPARATOR = "::"

class WdioTestLocationProvider : SMTestLocator {

    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope,
    ): List<Location<PsiElement>> {
        if (protocol != PROTOCOL) return emptyList()
        val filePath = path.substringBefore(PATH_SEPARATOR).takeIf { it.isNotBlank() } ?: return emptyList()
        val virtualFile = LocalFileSystem.getInstance()
            .findFileByPath(FileUtil.toSystemIndependentName(filePath))
            ?: return emptyList()
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return emptyList()
        return listOf(PsiLocation.fromPsiElement(psiFile))
    }
}
