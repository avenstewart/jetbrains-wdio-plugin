// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.runner

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginId
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private const val PLUGIN_ID = "com.perryweather.wdio"
private const val REPORTER_RESOURCE = "/wdio-reporter/wdio-intellij-reporter.cjs"
private const val REPORTER_FILENAME = "wdio-intellij-reporter.cjs"

object WdioReporterExtractor {

    fun extractedReporterPath(): Path {
        val version = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.version ?: "dev"
        val target = PathManager.getSystemDir()
            .resolve("wdio-intellij-reporter")
            .resolve(version)
            .resolve(REPORTER_FILENAME)
        if (!Files.exists(target)) {
            Files.createDirectories(target.parent)
            val resource = WdioReporterExtractor::class.java.getResourceAsStream(REPORTER_RESOURCE)
                ?: error("WDIO IntelliJ reporter resource not found at $REPORTER_RESOURCE")
            resource.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
        }
        return target
    }
}
