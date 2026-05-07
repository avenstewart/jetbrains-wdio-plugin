// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.

package com.perryweather.wdio.framework

object MochaAdapter : JsTestAdapter() {
    override val framework: Framework = Framework.MOCHA
    override val grepFlag: String = "--mochaOpts.grep"
    override val timeoutFlag: String = "--mochaOpts.timeout"
}
