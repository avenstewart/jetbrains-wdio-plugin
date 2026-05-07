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
        emit(buildMessage('testSuiteStarted', {
            nodeId,
            parentNodeId: this.currentParentNodeId(),
            name: suite.title || '',
            running: 'true',
            locationHint: this.locationHint(suite),
        }));
        this._suiteStack.push({ uid: suite.uid, nodeId });
    }

    onTestStart(test) {
        if (!test || !test.uid) return;
        const nodeId = this.nodeIdFor(test.uid);
        emit(buildMessage('testStarted', {
            nodeId,
            parentNodeId: this.currentParentNodeId(),
            name: test.title || '',
            running: 'true',
            locationHint: this.locationHint(test),
        }));
    }

    onTestPass(test) {
        if (!test || !test.uid) return;
        emit(buildMessage('testFinished', {
            nodeId: this.nodeIdFor(test.uid),
            duration: typeof test._duration === 'number' ? test._duration : undefined,
        }));
    }

    onTestFail(test) {
        if (!test || !test.uid) return;
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
        emit(buildMessage('testIgnored', {
            nodeId: this.nodeIdFor(test.uid),
            message: `Pending test '${test.title || ''}'`,
        }));
    }

    onSuiteEnd(suite) {
        if (!suite || !suite.uid) return;
        emit(buildMessage('testSuiteFinished', { nodeId: this.nodeIdFor(suite.uid) }));
        const top = this._suiteStack[this._suiteStack.length - 1];
        if (top && top.uid === suite.uid) this._suiteStack.pop();
    }

    onRunnerEnd() {
        while (this._suiteStack.length > 0) {
            const popped = this._suiteStack.pop();
            emit(buildMessage('testSuiteFinished', { nodeId: popped.nodeId }));
        }
        emit('##teamcity[testingFinished]');
    }
}

module.exports = WdioIntellijReporter;
module.exports.default = WdioIntellijReporter;
