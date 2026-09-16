Feature: Framework wiring

  The framework's own configuration and scenario context must work before any
  browser, device or HTTP client is introduced.

  @smoke
  Scenario: Framework configuration resolves for the active environment
    Given the framework configuration is loaded
    Then the web base URL is "http://localhost"
    And the default timeout is 5 seconds
    And the web base URL recorded earlier is readable from the scenario context
