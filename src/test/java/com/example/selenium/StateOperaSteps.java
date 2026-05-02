package com.example.selenium;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SpringBootTest
public class StateOperaSteps {

    private static final Logger log = LoggerFactory.getLogger(StateOperaSteps.class);
    private WebDriver driver;
    private Scenario scenario;

    @Before
    public void setUp(Scenario scenario) {
        this.scenario = scenario;
    }

    @Given("I open the Wiener Staatsoper calendar page for March 2026")
    public void i_open_the_calendar_page() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new"); // Run without a GUI
        options.addArguments("--no-sandbox"); // bypass OS security model (required for Docker)
        options.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
        options.addArguments("--remote-allow-origins=*"); // prevents 403 Forbidden errors
        driver = new ChromeDriver(options);
        driver.get("https://www.wiener-staatsoper.at/kalender/2026/maerz/");
        acceptCookies();
        takeScreenshot(driver, "page_loaded");
    }

    @When("I see the {string} popup, close it with {string}")
    public void i_see_the_popup_close_it_with(String popup, String button) {
        acceptCookies();
        takeScreenshot(driver, "after_accept_cookies");
    }

    @When("I scroll to the end of the page, I want to see each event with date and title")
    public void i_scroll_to_see_each_event_with_date_and_title() throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        long totalHeight = ((Number) js.executeScript("return document.body.scrollHeight")).longValue();
        long viewportHeight = ((Number) js.executeScript("return window.innerHeight")).longValue();
        long scrollPosition = 0L;

        log.info("Scrolling to end of page to view each event (total height: {}px)", totalHeight);

        int screenshotCount = 1;
        while (scrollPosition < totalHeight - viewportHeight) {
            scrollPosition = Math.min(scrollPosition + 500L, totalHeight - viewportHeight);
            js.executeScript("window.scrollTo(0, " + scrollPosition + ");");
            Thread.sleep(100);
            takeScreenshot(driver, "scroll_step_" + (screenshotCount++));
        }

        long newHeight = ((Number) Objects.requireNonNull(js.executeScript("return document.body.scrollHeight"))).longValue();
        while (newHeight > totalHeight) {
            log.info("Page content expanded to {}px, continuing scroll", newHeight);
            totalHeight = newHeight;
            while (scrollPosition < totalHeight - viewportHeight) {
                scrollPosition = Math.min(scrollPosition + 500L, totalHeight - viewportHeight);
                js.executeScript("window.scrollTo(0, " + scrollPosition + ");");
                Thread.sleep(200);
                takeScreenshot(driver, "scroll_step_" + (screenshotCount++));
            }
            Thread.sleep(1500);
            newHeight = ((Number) Objects.requireNonNull(js.executeScript("return document.body.scrollHeight"))).longValue();
        }

        log.info("Reached end of page, all events should be visible with date and title");

        List<String> events = extractEventsWithDateAndTitle(js);

        log.info("==================== EVENTS FOUND: {} ====================", events.size());
        for (String event : events) {
            log.info("### Event: {}", event);
        }
        log.info("===========================================================");

        takeScreenshot(driver, "all_events_visible");
    }

    @SuppressWarnings("unchecked")
    private List<String> extractEventsWithDateAndTitle(JavascriptExecutor js) {
        List<Map<String, String>> rawEvents = (List<Map<String, String>>) js.executeScript(
            "var events = [];" +
            "var containers = document.querySelectorAll('[class*=teaser], [class*=event-list], [class*=calendar], [class*=performance]');" +
            "containers.forEach(function(container) {" +
            "  var titleEl = container.querySelector('h2, h3, h4, [class*=title]');" +
            "  var title = titleEl ? titleEl.textContent.trim() : '';" +
            "  var dateParts = [];" +
            "  var dateEls = container.querySelectorAll('time, [class*=date], [class*=day]');" +
            "  dateEls.forEach(function(d) { dateParts.push(d.textContent.trim()); });" +
            "  if (title && title.length > 3 && title.length < 80) {" +
            "    events.push({" +
            "      title: title," +
            "      date: dateParts.join(' ')" +
            "    });" +
            "  }" +
            "});" +
            "if (events.length === 0) {" +
            "  var allTexts = document.querySelectorAll('h2, h3');" +
            "  allTexts.forEach(function(el) {" +
            "    var t = el.textContent.trim();" +
            "    if (t.length > 3 && t.length < 80) {" +
            "      events.push({title: t, date: ''});" +
            "    }" +
            "  });" +
            "}" +
            "return events;"
        );

        List<String> result = new ArrayList<>();
        for (int i = 0; i < rawEvents.size(); i++) {
            Map<String, String> event = rawEvents.get(i);
            String date = event.getOrDefault("date", "").replaceAll("\\s+", " ").trim();
            String title = event.getOrDefault("title", "").replaceAll("\\s+", " ").trim();
            if (date.isEmpty()) {
                result.add(String.format("%d. %s", i + 1, title));
            } else {
                result.add(String.format("%d. [%s] %s", i + 1, date, title));
            }
        }
        return result;
    }

    @Then("the last event this month should be {string}")
    public void the_last_event_this_month_should_be(String expectedEventTitle) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<String> events = extractEventsWithDateAndTitle(js);

        Assertions.assertFalse(events.isEmpty(), "No events found on the page");

        String lastEvent = events.getLast();
        log.info("Last event on page: {}", lastEvent);

        boolean found = lastEvent.toLowerCase().contains(expectedEventTitle.toLowerCase());

        if (!found) {
            log.info("Expected '{}' not found in last event '{}'", expectedEventTitle, lastEvent);
            takeScreenshot(driver, "last_event_mismatch");
        }

        Assertions.assertTrue(found,
            String.format("Expected last event to contain '%s' but was '%s'", expectedEventTitle, lastEvent));

        takeScreenshot(driver, "last_event_verified");
    }

    @Then("I should find at least one Details button")
    public void i_should_find_at_least_one_details_button() {
        List<WebElement> detailsButtons = driver.findElements(
            By.xpath("//button[contains(text(), 'Details')] | //a[contains(text(), 'Details')]")
        );
        log.info("SUCCESS: Found {} Details button(s)", detailsButtons.size());
        Assertions.assertFalse(detailsButtons.isEmpty(), "Expected to find Details buttons on the page");
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void takeScreenshot(WebDriver driver, String name) {
        try {
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path targetPath = Path.of("target/" + name + ".png");
            Files.copy(screenshotFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            if (scenario != null) {
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                scenario.attach(screenshotBytes, "image/png", name);
            }

            log.info("Screenshot saved: {}", targetPath);
        } catch (IOException e) {
            log.error("Failed to save screenshot: {}", e.getMessage());
        }
    }

    private void acceptCookies() {
        try {
            Thread.sleep(1000);
            List<WebElement> acceptButtons = driver.findElements(
                By.xpath("//button[contains(text(), 'Alle Akzeptieren')] | //button[contains(text(), 'Alle akzeptieren')] | //a[contains(text(), 'Alle Akzeptieren')] | //div[contains(@class, 'cookie')]//button")
            );
            if (!acceptButtons.isEmpty()) {
                acceptButtons.getFirst().click();
                log.info("Cookie popup closed");
            }
        } catch (Exception e) {
            log.warn("Could not find cookie popup: {}", e.getMessage());
        }
    }
}
