// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Perry Weather, Inc.
//
// WebdriverIO reporter that emits TeamCity ServiceMessages so the JetBrains
// SMTRunner test tree shows live pass/fail/skip state for tests running under
// the WebdriverIO IDE plugin.

'use strict';

const path = require('path');
const Module = require('module');

function loadWdioReporter() {
    try {
        return require('@wdio/reporter').default;
    } catch (_) {
        const projectRequire = Module.createRequire(path.join(process.cwd(), 'package.json'));
        return projectRequire('@wdio/reporter').default;
    }
}

const WDIOReporter = loadWdioReporter();

const ESC_MAP = {
    10: 'n',     // \n
    13: 'r',     // \r
    0x85: 'x',
    0x2028: 'l',
    0x2029: 'p',
    124: '|',    // |
    39: "'",     // '
    91: '[',     // [
    93: ']',     // ]
};

function escapeAttr(input) {
    if (input == null) return '';
    const str = String(input);
    let out = '';
    for (let i = 0; i < str.length; i++) {
        const code = str.charCodeAt(i);
        const esc = ESC_MAP[code];
        if (esc !== undefined) {
            out += '|' + esc;
        } else {
            out += str.charAt(i);
        }
    }
    return out;
}

function emit(line) {
    process.stdout.write(line + '\n');
}

function buildMessage(command, attrs) {
    let msg = '##teamcity[' + command;
    for (const [key, value] of Object.entries(attrs)) {
        if (value === undefined || value === null) continue;
        msg += ` ${key}='${escapeAttr(value)}'`;
    }
    msg += ']';
    return msg;
}

class WdioIntellijReporter extends WDIOReporter {
    constructor(options) {
        super(Object.assign({ stdout: true }, options || {}));
        this._nextNodeId = 1;
        this._nodeIds = new Map();        // uid -> nodeId
        this._suiteStack = [];            // [{uid, nodeId}] currently open suites in nesting order
        this._bufferedStarts = new Map(); // uid -> testStarted attrs (flushed on pass/fail, dropped on skip)
        this._bufferedSuiteStarts = new Map(); // uid -> testSuiteStarted attrs (flushed when first child runs)
        this._suitesEmitted = new Set();  // suite uids that already emitted testSuiteStarted
    }

    nodeIdFor(uid) {
        let id = this._nodeIds.get(uid);
        if (id === undefined) {
            id = this._nextNodeId++;
            this._nodeIds.set(uid, id);
        }
        return id;
    }

    currentParentNodeId() {
        if (this._suiteStack.length === 0) return 0;
        return this._suiteStack[this._suiteStack.length - 1].nodeId;
    }

    locationHint(item) {
        if (!item || !item.file) return undefined;
        const fullName = item.fullTitle || item.title || '';
        return `wdio://${item.file}::${fullName}`;
    }

    onRunnerStart() {
        emit('##teamcity[testingStarted]');
    }

    onSuiteStart(suite) {
        if (!suite || !suite.uid) return;
        const nodeId = this.nodeIdFor(suite.uid);
        // Defer testSuiteStarted in the same way we defer testStarted: only emit once
        // we know a child actually ran. Empty suites (e.g. parents whose only tests were
        // grep-filtered) shouldn't appear in the tree at all.
        this._bufferedSuiteStarts.set(suite.uid, {
            nodeId,
            parentNodeId: this.currentParentNodeId(),
            name: suite.title || '',
            running: 'true',
            locationHint: this.locationHint(suite),
        });
        this._suiteStack.push({ uid: suite.uid, nodeId });
    }

    flushSuiteAncestors() {
        for (const entry of this._suiteStack) {
            if (this._suitesEmitted.has(entry.uid)) continue;
            const attrs = this._bufferedSuiteStarts.get(entry.uid);
            if (!attrs) continue;
            emit(buildMessage('testSuiteStarted', attrs));
            this._suitesEmitted.add(entry.uid);
            this._bufferedSuiteStarts.delete(entry.uid);
        }
    }

    onTestStart(test) {
        if (!test || !test.uid) return;
        // Buffer the testStarted message. Some frameworks (notably @wdio/jasmine-framework)
        // fire onTestStart for grep-filtered tests too, then immediately fire onTestSkip;
        // emitting eagerly would clutter the SMTRunner tree with "pending" rows for tests
        // the user never asked to run. The buffered message is flushed on pass/fail and
        // discarded on skip.
        this._bufferedStarts.set(test.uid, {
            nodeId: this.nodeIdFor(test.uid),
            parentNodeId: this.currentParentNodeId(),
            name: test.title || '',
            running: 'true',
            locationHint: this.locationHint(test),
        });
    }

    flushBufferedStart(uid) {
        const attrs = this._bufferedStarts.get(uid);
        if (!attrs) return;
        this.flushSuiteAncestors();
        emit(buildMessage('testStarted', attrs));
        this._bufferedStarts.delete(uid);
    }

    onTestPass(test) {
        if (!test || !test.uid) return;
        this.flushBufferedStart(test.uid);
        emit(buildMessage('testFinished', {
            nodeId: this.nodeIdFor(test.uid),
            duration: typeof test._duration === 'number' ? test._duration : undefined,
        }));
    }

    onTestFail(test) {
        if (!test || !test.uid) return;
        this.flushBufferedStart(test.uid);
        const error = (test.errors && test.errors[0]) || test.error || {};
        emit(buildMessage('testFailed', {
            nodeId: this.nodeIdFor(test.uid),
            duration: typeof test._duration === 'number' ? test._duration : undefined,
            message: error.message || 'Test failed',
            details: error.stack || '',
        }));
    }

    onTestSkip(test) {
        if (!test || !test.uid) return;
        // Drop the buffered testStarted so the test doesn't appear in the tree at all.
        // This covers grep-filtered specs as well as xit/xdescribe pending tests.
        this._bufferedStarts.delete(test.uid);
    }

    onSuiteEnd(suite) {
        if (!suite || !suite.uid) return;
        if (this._suitesEmitted.has(suite.uid)) {
            emit(buildMessage('testSuiteFinished', { nodeId: this.nodeIdFor(suite.uid) }));
            this._suitesEmitted.delete(suite.uid);
        }
        // If the suite never ran any visible children, drop the buffered start without emitting.
        this._bufferedSuiteStarts.delete(suite.uid);
        const top = this._suiteStack[this._suiteStack.length - 1];
        if (top && top.uid === suite.uid) this._suiteStack.pop();
    }

    onRunnerEnd() {
        while (this._suiteStack.length > 0) {
            const popped = this._suiteStack.pop();
            if (this._suitesEmitted.has(popped.uid)) {
                emit(buildMessage('testSuiteFinished', { nodeId: popped.nodeId }));
                this._suitesEmitted.delete(popped.uid);
            }
            this._bufferedSuiteStarts.delete(popped.uid);
        }
        emit('##teamcity[testingFinished]');
    }
}

module.exports = WdioIntellijReporter;
module.exports.default = WdioIntellijReporter;
