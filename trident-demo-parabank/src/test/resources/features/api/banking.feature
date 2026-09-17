Feature: Banking operations over the REST API

  Every scenario registers its own customer and works only on the accounts that customer
  owns, so no scenario depends on another having run, or on the order they run in.

  @api
  Scenario: Login with valid credentials returns the customer
    Given I have registered a customer
    When I log in with my own credentials
    Then the API returns my customer record

  @api
  Scenario: Opening an account returns an account belonging to me
    Given I have registered a customer
    And I have opened a second account funded from my first
    Then the new account belongs to me and is not my first

  @api
  Scenario: A transfer moves the balance of both accounts
    Given I have registered a customer
    And I have opened a second account funded from my first
    When I transfer 250 from my first account to my second
    Then 250 has moved from my first account to my second

  @api
  Scenario: The transfer appears in the receiving account's transactions
    Given I have registered a customer
    And I have opened a second account funded from my first
    When I transfer 250 from my first account to my second
    Then my second account's transactions include a credit of 250

  @api
  Scenario: Login with the wrong password is rejected
    Given I have registered a customer
    When I log in with the wrong password
    Then the API rejects the login

  # ParaBank has no overdraft protection. The scenario asserts what it does, not what a bank
  # should do; see the comment on the step for why, and what to do if it is ever fixed.
  @api
  Scenario: A transfer beyond the balance is accepted, and overdraws the account
    Given I have registered a customer
    And I have opened a second account funded from my first
    When I transfer 10000000 from my first account to my second
    Then ParaBank accepts it and overdraws my account
