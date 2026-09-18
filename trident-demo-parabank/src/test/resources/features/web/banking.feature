Feature: Banking through the browser

  The same bank as the @api scenarios, driven the way a customer drives it. Each scenario
  creates its own customer over REST and then uses only the browser, so what is being tested
  is the interface and not the fixture.

  @web
  Scenario: A customer logs in and lands on their account overview
    Given a registered customer
    When they log in through the browser
    Then they see their own account in the overview

  @web
  Scenario: A customer opens a second account and sees it listed
    Given a registered customer
    And they are logged in through the browser
    When they open a savings account funded from their first account
    Then the new account appears in the overview

  @web
  Scenario: A transfer moves money between two of the customer's accounts
    Given a registered customer with two accounts
    And they are logged in through the browser
    When they transfer $50.00 from the first account to the second
    Then both balances have moved by $50.00

  @web
  Scenario: A wrong password is refused with the message the page shows
    Given a registered customer
    When they log in through the browser with the wrong password
    Then the page says the username and password could not be verified
