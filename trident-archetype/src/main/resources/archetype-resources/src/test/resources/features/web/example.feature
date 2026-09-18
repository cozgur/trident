Feature: The browser suite is wired correctly

  One scenario that proves the browser half of the plumbing: a driver starts, a page object
  waits for its own loaded condition, and a locator built the way Trident recommends finds a
  control. It renders its own page, so it needs no server and no application - if it passes,
  your browser and driver are set up correctly.

  Nothing here runs by default. `mvn verify` uses the smoke profile and skips this suite, so a
  freshly generated project is green on a machine with no browser. Run it with:

      mvn -Pweb verify

  Replace this scenario once you have a screen of your own to drive.

  @web
  Scenario: A page object waits for its page and finds a control
    When I open the example page
    And I enter the reference "TRIDENT-1"
    Then the page is headed "Trident"
