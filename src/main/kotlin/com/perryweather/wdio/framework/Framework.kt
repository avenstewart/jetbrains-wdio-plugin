// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

enum class Framework(val cliName: String, val displayName: String) {
    MOCHA(cliName = "mocha", displayName = "Mocha"),
    JASMINE(cliName = "jasmine", displayName = "Jasmine"),
    CUCUMBER(cliName = "cucumber", displayName = "Cucumber");

    companion object {
        fun fromString(value: String?): Framework {
            val v = value?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return MOCHA
            return entries.firstOrNull { it.cliName == v } ?: MOCHA
        }
    }
}