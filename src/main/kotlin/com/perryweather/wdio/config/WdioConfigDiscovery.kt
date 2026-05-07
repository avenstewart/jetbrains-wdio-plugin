// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.config

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

object WdioConfigDiscovery {
    private const val MAX_DEPTH = 5
    private val FILENAME_REGEX = Regex("""^wdio.*\.conf\.(ts|js|mjs|cjs)$""", RegexOption.IGNORE_CASE)
    private val SKIP_DIRS = setOf("node_modules", ".git", "build", "dist", "out", "target", ".idea", ".gradle")

    fun discover(root: Path): List<Path> {
        if (!root.isDirectory()) return emptyList()
        val out = mutableListOf<Path>()
        walk(root, depth = 0, out = out)
        return out.sortedBy { it.toString() }
    }

    private fun walk(dir: Path, depth: Int, out: MutableList<Path>) {
        if (depth >= MAX_DEPTH) return
        Files.list(dir).use { stream ->
            stream.forEach { child ->
                if (child.isDirectory()) {
                    if (child.name !in SKIP_DIRS) walk(child, depth + 1, out)
                } else if (FILENAME_REGEX.matches(child.name)) {
                    out.add(child)
                }
            }
        }
    }
}
