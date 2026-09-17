Feature: The suite is wired correctly

  A first scenario that asserts something real: that Trident's configuration resolves, and
  that a value written to the scenario context in one step is still there in a later one. If
  this passes, the plumbing works and you can start writing your own features.

  Replace it once you have a scenario of your own. Keep one Docker-free @smoke scenario,
  though - it is what tells a newcomer whether their setup is broken or your product is.

  @smoke
  Scenario: Configuration resolves and the scenario context carries a value
    Given the configuration is loaded
    Then the environment is "local"
    And the scenario context still holds "local|http://localhost"
