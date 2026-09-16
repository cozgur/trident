Feature: Framework wiring

  The framework's own configuration and scenario context must work before any
  browser, device or HTTP client is introduced.

  @smoke
  Scenario: Framework configuration resolves for the active environment
    Given the framework configuration is loaded
    Then the web base URL is "http://localhost"
    And the default timeout is 5 seconds
    # The step below reads back a value that the first step composed and wrote to the
    # ScenarioContext. That string appears in no property file, so it can only exist if a
    # step body actually executed: a reported PASSED status is not enough to produce it.
    # It is what makes a real run and a dry run differ in observable state rather than only
    # in what the report claims.
    And the scenario context holds the resolved configuration "local|http://localhost|5"
