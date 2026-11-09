# Changelog

Only releases on Maven Central are listed. Every version is immutable once published; see
[releasing](docs/releasing.md).

## 1.1.0

The web layer. In 1.0.0, `trident-web` was published as an empty module; this release fills it.

- **`trident-web` (Selenium 4).** `DriverProvider` and `WebDriverFactory` own the browser's
  lifecycle; `LoadablePage` is the page-object base, and every page declares the condition that
  means it has finished loading. Selenium Manager resolves the browser driver. No locators ship
  with the framework: they belong to the consumer, and
  [ADR 0017](docs/adr/0017-locator-strategy-for-an-unmodifiable-ui.md) sets out how to write them.
- **Two new configuration keys**, `browser` and `browser.headless`, which makes seven in total.
- **The archetype generates the web suite.** A generated project has a third suite,
  `-Pweb`, with its own reports and message log. A default build skips it, so a freshly
  generated project is green on a machine with no browser.
- **Allure for all suites in the reference implementation.** `trident-demo-parabank` reports
  its smoke, api and web runs to Allure. No published module depends on a report adapter, and
  generated projects carry none: which report to use is the consumer's choice.
- **Parallel execution is wired and switched off.** Scenario-level parallelism is one property
  away, but the reference target cannot create two rows at once, so the suite passed 1 of 20
  parallel web runs and 0 of 20 api runs. Measured in
  [ADR 0018](docs/adr/0018-parallel-execution-at-scenario-level.md); the alternative that was
  tried and deleted is in [ADR 0019](docs/adr/0019-one-container-per-thread.md).
- **The release workflow runs the web suite** and its execution gate before it publishes.

Not in this release: `trident-mobile` is still a placeholder. Mobile is Phase 4.
