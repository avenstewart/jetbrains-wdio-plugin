// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

sealed interface TestFilter {
    object None : TestFilter

    // Mocha / Jasmine: match test name(s) by regex via --{moch,jasmin}aOpts.grep.
    data class Grep(val pattern: String) : TestFilter

    // Cucumber: tag expression via --cucumberOpts.tags (e.g. "@smoke and not @wip").
    data class CucumberTags(val expression: String) : TestFilter

    // Cucumber: scenario name regex via --cucumberOpts.name. Used both for "run this scenario"
    // (anchored ^name$) and free-form name filters.
    data class CucumberName(val pattern: String) : TestFilter
}
