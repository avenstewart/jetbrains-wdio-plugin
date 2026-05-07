// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

internal fun escapeRegex(s: String): String = buildString(s.length) {
    for (c in s) {
        when (c) {
            '\\', '.', '+', '*', '?', '(', ')', '{', '}', '[', ']', '^', '$', '|' -> {
                append('\\'); append(c)
            }
            else -> append(c)
        }
    }
}
