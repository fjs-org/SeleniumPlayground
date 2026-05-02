Feature: Wiener Staatsoper Calendar

  Scenario: Verify Details button exists on calendar page
    Given I open the Wiener Staatsoper calendar page for March 2026
    When I see the "Diese Website nutzt Cookies." popup, close it with "Alle Akzeptieren"
    When I scroll to the end of the page, I want to see each event with date and title
    Then the last event this month should be "Die verkaufte Braut"
