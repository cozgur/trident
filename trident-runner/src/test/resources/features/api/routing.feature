Feature: Integration suite routing

  A temporary placeholder that proves the Failsafe suite is wired and tag-routed before any
  container-backed scenario exists. It reuses the smoke suite's steps deliberately, so it
  needs no Docker.

  CP 1.5 deletes this file. It is tagged @wip as well as @api so that regression excludes it
  and its removal is not merely an intention: the Phase 1 gate requires this scenario to be
  gone once real ParaBank scenarios exist.

  @api @wip
  Scenario: The integration suite resolves the framework configuration
    Given the framework configuration is loaded
    Then the web base URL is "http://localhost"
