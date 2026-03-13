Feature: Wiener Staatsoper Calendar

  Scenario: Verify Details button exists on calendar page
    Given I open the Wiener Staatsoper calendar page for March 2026
    When I see the "Diese Website nutzt Cookies." popup, close it with "Alle Akzeptieren"
    When I search for buttons with text "Details"
    Then I should find at least one Details button
