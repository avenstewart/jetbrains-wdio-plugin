# jetbrains-wdio-plugin

Run WebdriverIO tests directly from the editor in WebStorm and IntelliJ IDEA Ultimate.

<!-- Plugin description -->
Run WebdriverIO tests directly from the editor in WebStorm and IntelliJ IDEA Ultimate.

Adds a WebdriverIO run configuration that invokes <code>@wdio/cli</code> with the correct arguments so the WDIO runner, capabilities, services, and hooks all load. Supports Mocha, Jasmine, and Cucumber/Gherkin, with gutter Run arrows on test files and feature files.
<!-- Plugin description end -->

## Why this plugin exists

WebStorm bundles a Cucumber.js plugin that draws gutter Run arrows on `.feature` files.
Clicking those arrows shells out to `cucumber-js` directly. For projects using
`@wdio/cucumber-framework`, that path is broken: the WDIO runner, capabilities,
services, and hooks never load, so the scenario errors out before it ever talks to
a browser.

The bundled Mocha plugin has the same shape on `*.spec.js` files — its gutter arrows
go through `mocha`, not `wdio`, so any WDIO config or hooks are skipped.

This plugin adds a **WebdriverIO** run configuration that invokes `@wdio/cli` with
the correct argv so a click on a gutter arrow runs the test the way `wdio run` would.

## Features

- Gutter Run arrows on `describe`/`it`/`Scenario`/`Feature` produce a WebdriverIO
  run config (not a Mocha or Cucumber.js one) when `@wdio/cli` is present in
  `node_modules`.
- Mocha and Jasmine specs target a single test via `--{moch,jasmin}aOpts.grep`.
- Cucumber scenarios target a single scenario via `--cucumberOpts.name`. Tags
  use `--cucumberOpts.tags`.
- Multi-config picker in the run-config editor: every `wdio*.conf.{ts,js,mjs,cjs}`
  in the working directory is offered as a dropdown option, with auto-select when
  exactly one match exists. Free-typed paths still work.
- Framework auto-detection: the producer reads `framework: '...'` out of the
  selected `wdio.conf.*` and pre-fills the framework dropdown.
- Live test tree: a bundled Node-side reporter emits TeamCity ServiceMessages so
  the SMTRunner test view shows pass/fail/skip with stack traces, durations, and
  click-to-source navigation.
- Debug executor wired through `--inspect-brk` so breakpoints inside step
  definitions, hooks, and specs are honored.
- Rerun Failed Tests action.

## Supported IDEs and versions

- **WebStorm 2025.1.x**
- **IntelliJ IDEA Ultimate 2025.1.x**

The plugin is built against WebStorm 2025.1 because WebStorm bundles the
Gherkin language plugin we depend on. IDEA Ultimate is supported at runtime —
install the JetBrains "Gherkin" plugin from the Marketplace first if you want
`.feature` gutter arrows; the plugin loads either way and the Gherkin
integration activates automatically when the dependency is present.

JetBrains restructured the JS test framework API between 2025.1 and 2025.2, so
the plugin currently declares incompatibility with 2025.2 and newer to avoid
runtime errors. Compatibility with later builds will follow once the affected
detector code is rewritten.

## Install

The plugin is not on the JetBrains Marketplace yet. To install from a local build:

1. Clone this repository.
2. Run `./gradlew buildPlugin`. The packaged distribution lands in
   `build/distributions/jetbrains-wdio-plugin-<version>.zip`.
3. In your IDE: **Settings -> Plugins -> [gear icon] -> Install Plugin from Disk...**
   and pick that zip.
4. Restart the IDE when prompted.

## Quickstart

### Run a Mocha or Jasmine spec

1. Open a project that has `@wdio/cli` installed and a `wdio.conf.*` configured
   for `framework: 'mocha'` or `'jasmine'`.
2. Open a `.spec.js`/`.spec.ts` file. Click the gutter arrow next to an `it(...)`
   block. A WebdriverIO run config is created and runs that single test.
3. The test tree populates with each `describe` -> `it`. Pass/fail/error states
   render with stack traces.

### Run a Cucumber scenario

1. Open a project that has `@wdio/cli` and `@wdio/cucumber-framework` installed,
   with `framework: 'cucumber'` in your `wdio.conf.*`.
2. Open a `.feature` file. Click the gutter arrow next to a `Scenario:` line.
3. Right-click the gutter and choose **Run 'Scenario name' (WebdriverIO)** if the
   bundled Cucumber.js plugin is also present. The plugin claims priority over
   Cucumber.js when WDIO is detected, so a left-click usually produces the right
   thing without the menu.

### Run a tagged subset

1. Click the gutter arrow next to a `@tag` line on a Scenario.
2. The generated config uses `--cucumberOpts.tags="@tag"`.
3. Edit the run config to combine tag expressions like `"@smoke and not @wip"`
   if you want a custom selection.

## Configuration reference

The Edit Configurations dialog exposes:

- **Node interpreter** — inherits the project default; override per-config if needed.
- **Node options** — extra flags passed to `node` (e.g. `--require ts-node/register`).
- **WebdriverIO package** — auto-resolved from `node_modules/@wdio/cli` for the current project.
- **Working directory** — inherits the project base path; the producer guesses a closer directory when running from a subfolder.
- **Environment variables** — standard env-var dialog. Anything in here lands in the wdio process env.
- **WDIO config file** — dropdown populated with every discovered `wdio*.conf.{ts,js,mjs,cjs}`.
- **Framework** — auto-detected from the chosen `wdio.conf.*`. Manual override available.
- **Test file** — set automatically by the gutter producer to the clicked spec/feature file.

## How the wdio command is built

The plugin produces argv that maps cleanly to a `wdio run` invocation. Roughly:

```
node <node-options> <node_modules/@wdio/cli/bin/wdio.js> run <wdio.conf.*> \
     --framework <mocha|jasmine|cucumber> \
     [--mochaOpts.grep "^Suite test$"     # Mocha single test ]
     [--jasmineOpts.grep "^Suite test$"   # Jasmine single test ]
     [--cucumberOpts.name "^Scenario$"    # Cucumber single scenario ]
     [--cucumberOpts.tags "@expr"         # Cucumber tag selection ]
     --reporters <bundled-intellij-reporter.cjs> \
     [--reporters <reporters from wdio.conf>     # additive, set by user ]
     --spec <absolute path to clicked file>
```

Debug runs add `--{moch,jasmin,cucumber}Opts.timeout 0` so breakpoints don't
trigger framework timeouts.

The IntelliJ reporter is shipped as a small Node package extracted at runtime
into the IDE's system path; nothing about it requires you to add a reporter to
your `wdio.conf.*`. WDIO merges it with whatever reporters are already
configured.

## Comparison to the bundled Cucumber.js plugin

**This plugin** invokes `@wdio/cli`, so the `wdio.conf` services, WebDriver
capabilities, and `Before`/`After` hooks defined for WDIO all run on each
scenario. Use it when your project depends on `@wdio/cucumber-framework`.

**The bundled Cucumber.js plugin** invokes `cucumber-js` directly. None of
WDIO's services, capabilities, or WDIO-side hooks load. Use it when your
project uses raw `@cucumber/cucumber` without WDIO.

If your project genuinely uses `cucumber-js` directly (no WDIO), keep using the
bundled plugin. This one is for `@wdio/cucumber-framework` projects.

## Troubleshooting

**No gutter arrows on a `.spec.js` file.** The plugin relies on JetBrains' bundled
Mocha/Jasmine PSI recognition; it activates when `@wdio/cli` is present in
`node_modules` (search walks upward from the file). Run `npm install` if your
node_modules is missing or stale.

**No gutter arrows on a `.feature` file.** Make sure the JetBrains "Gherkin"
plugin is enabled. WebStorm bundles it; IDEA Ultimate users install it from the
Marketplace.

**"Test framework quit unexpectedly" with no test tree.** The bundled WDIO
reporter failed to load. Check the run console for a Node `require()` error and
confirm `@wdio/reporter` resolves from your project's `node_modules`.

**Run config opens but the run errors with "Cannot resolve @wdio/cli bin file".**
The WebdriverIO package field is empty or pointing at a path without a `bin/wdio.js`
entry. Open the run config and re-pick `node_modules/@wdio/cli`.

**Cucumber scenario click runs the wrong scenario.** Our adapter generates a
`--cucumberOpts.name` regex anchored to the exact scenario title. If your scenario
title contains regex metacharacters, the editor lets you tweak the generated
config — open Run -> Edit Configurations and adjust the test file path or
framework filter as needed.

## Contributing

Build:

```
./gradlew build
```

Run tests:

```
./gradlew test
```

Launch a sandbox IDE with the plugin loaded:

```
./gradlew runIde
```

Run the JetBrains Plugin Verifier:

```
./gradlew verifyPlugin
```

## License and attribution

Apache-2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

This plugin started as a clean-slate rewrite informed by the architecture of
[winkingzhang/idea-run-wdio](https://github.com/winkingzhang/idea-run-wdio),
also Apache-2.0 licensed. Maintained by Perry Weather, Inc.
([perryweather.com](https://perryweather.com)).
