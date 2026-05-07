// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.openapi.application.PathManager
import java.nio.file.Files
import java.nio.file.Path

private const val REPORTER_RESOURCE = "/wdio-reporter/wdio-intellij-reporter.cjs"
private const val REPORTER_FILENAME = "wdio-intellij-reporter.cjs"

object WdioReporterExtractor {

    fun extractedReporterPath(): Path {
        val bytes = WdioReporterExtractor::class.java.getResourceAsStream(REPORTER_RESOURCE)
            ?.use { it.readAllBytes() }
            ?: error("WDIO IntelliJ reporter resource not found at $REPORTER_RESOURCE")
        val tag = Integer.toHexString(bytes.contentHashCode())
        val target = PathManager.getSystemDir()
            .resolve("wdio-intellij-reporter")
            .resolve(tag)
            .resolve(REPORTER_FILENAME)
        if (!Files.exists(target)) {
            Files.createDirectories(target.parent)
            Files.write(target, bytes)
        }
        return target
    }
}
