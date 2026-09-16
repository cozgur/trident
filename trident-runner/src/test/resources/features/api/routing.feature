Feature: Integration suite routing

  A placeholder that proves the Failsafe suite is wired and tag-routed before any
  container-backed scenario exists. It reuses the smoke suite's steps deliberately, so it
  needs no Docker. CP 1.5 replaces it with real ParaBank scenarios.

  @api
  Scenario: The integration suite resolves the framework configuration
    Given the framework configuration is loaded
    Then the web base URL is "http://localhost"
