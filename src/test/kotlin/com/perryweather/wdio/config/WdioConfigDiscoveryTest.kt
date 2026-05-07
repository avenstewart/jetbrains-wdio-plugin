// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class WdioConfigDiscoveryTest {

    @Test
    fun `empty directory returns empty list`(@TempDir root: Path) {
        assertEquals(emptyList<Path>(), WdioConfigDiscovery.discover(root))
    }

    @Test
    fun `single config at root is discovered`(@TempDir root: Path) {
        val config = root.resolve("wdio.conf.js").also { it.createFile() }
        assertEquals(listOf(config), WdioConfigDiscovery.discover(root))
    }

    @Test
    fun `all four extensions are recognized`(@TempDir root: Path) {
        listOf("wdio.conf.ts", "wdio.conf.js", "wdio.conf.mjs", "wdio.conf.cjs")
            .forEach { root.resolve(it).createFile() }
        val results = WdioConfigDiscovery.discover(root).map { it.fileName.toString() }
        assertEquals(listOf("wdio.conf.cjs", "wdio.conf.js", "wdio.conf.mjs", "wdio.conf.ts"), results)
    }

    @Test
    fun `wildcard prefixes match the consumer-suite naming convention`(@TempDir root: Path) {
        val configDir = root.resolve("config").also { it.createDirectories() }
        listOf(
            "wdio.web.conf.ts",
            "wdio.android.native.conf.ts",
            "wdio.ios.native.conf.ts",
            "wdio.browserstack.conf.ts",
        ).forEach { configDir.resolve(it).createFile() }
        val results = WdioConfigDiscovery.discover(root).map { it.fileName.toString() }.sorted()
        assertEquals(
            listOf("wdio.android.native.conf.ts", "wdio.browserstack.conf.ts", "wdio.ios.native.conf.ts", "wdio.web.conf.ts"),
            results,
        )
    }

    @Test
    fun `nested config files are discovered up to depth limit`(@TempDir root: Path) {
        val deep = root.resolve("a/b/c/d").also { it.createDirectories() }
        deep.resolve("wdio.conf.js").createFile()
        val results = WdioConfigDiscovery.discover(root)
        assertEquals(1, results.size)
    }

    @Test
    fun `files past max depth are not discovered`(@TempDir root: Path) {
        val tooDeep = root.resolve("a/b/c/d/e/f").also { it.createDirectories() }
        tooDeep.resolve("wdio.conf.js").createFile()
        val results = WdioConfigDiscovery.discover(root)
        assertEquals(emptyList<Path>(), results)
    }

    @Test
    fun `node_modules is skipped`(@TempDir root: Path) {
        val nodeModules = root.resolve("node_modules/somepkg").also { it.createDirectories() }
        nodeModules.resolve("wdio.conf.js").createFile()
        val rootConfig = root.resolve("wdio.conf.js").also { it.createFile() }
        val results = WdioConfigDiscovery.discover(root)
        assertEquals(listOf(rootConfig), results)
    }

    @Test
    fun `unrelated files are not discovered`(@TempDir root: Path) {
        listOf("wdio.conf.json", "package.json", "tsconfig.json", "wdio-config.ts", "config.wdio.ts")
            .forEach { root.resolve(it).createFile() }
        assertEquals(emptyList<Path>(), WdioConfigDiscovery.discover(root))
    }

    @Test
    fun `non-existent root returns empty list`() {
        val nope = Path.of("/tmp/this-does-not-exist-${System.nanoTime()}")
        assertEquals(emptyList<Path>(), WdioConfigDiscovery.discover(nope))
    }

    @Test
    fun `regular file as root returns empty list`(@TempDir root: Path) {
        val file = root.resolve("a-file.txt").also { it.createFile() }
        assertEquals(emptyList<Path>(), WdioConfigDiscovery.discover(file))
    }

    @Test
    fun `results are sorted by path`(@TempDir root: Path) {
        root.resolve("z/wdio.conf.js").apply { parent.createDirectories(); createFile() }
        root.resolve("a/wdio.conf.js").apply { parent.createDirectories(); createFile() }
        root.resolve("wdio.conf.js").createFile()
        val paths = WdioConfigDiscovery.discover(root).map { it.toString() }
        assertEquals(paths, paths.sorted())
        assertTrue(paths.first().endsWith("wdio.conf.js"))
    }
}
