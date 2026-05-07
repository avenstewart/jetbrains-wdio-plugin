// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

object JasmineAdapter : JsTestAdapter() {
    override val framework: Framework = Framework.JASMINE
    override val grepFlag: String = "--jasmineOpts.grep"
    override val timeoutFlag: String = "--jasmineOpts.defaultTimeoutInterval"
}
