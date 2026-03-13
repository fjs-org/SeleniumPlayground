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
import java.util.List;

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
        options.addArguments("--headless=new"); // The 'new' flag is recommended for latest Chrome
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
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

    @When("I search for buttons with text {string}")
    public void i_search_for_buttons_with_text(String buttonText) {
        List<WebElement> buttons = driver.findElements(
            By.xpath("//button[contains(text(), '" + buttonText + "')] | //a[contains(text(), '" + buttonText + "')]")
        );
        log.info("Found {} button(s) with text '{}'", buttons.size(), buttonText);
        takeScreenshot(driver, "buttons_found");
        Assertions.assertFalse(buttons.isEmpty(), "Expected to find buttons with text '" + buttonText + "'");
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
