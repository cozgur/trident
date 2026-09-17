Feature: Scenario-owned test data

  Every scenario creates the customer it needs and reads nothing another created. These two
  scenarios are deliberately alike: run in either order, neither may see the other's data.
  If one of them only passes when it runs first, the isolation is not real.

  @api
  Scenario: A scenario creates and owns its customer
    Given this scenario has no customer yet
    When I register a new customer
    Then the customer can log in with their own credentials
    And the customer owns exactly one account, and it is theirs

  @api
  Scenario: A second scenario is unaffected by the first
    Given this scenario has no customer yet
    When I register a new customer
    Then the customer can log in with their own credentials
    And the customer owns exactly one account, and it is theirs
