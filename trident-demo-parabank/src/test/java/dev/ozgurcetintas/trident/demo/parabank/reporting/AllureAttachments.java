package dev.ozgurcetintas.trident.demo.parabank.reporting;

import dev.ozgurcetintas.trident.demo.parabank.fixtures.ApiExchangeLog;
import dev.ozgurcetintas.trident.web.DriverProvider;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Attaches evidence to the Allure report, and only when a scenario has failed.
 *
 * <p>A passing run attaches nothing. That is a deliberate constraint rather than an oversight:
 * attaching a screenshot to every scenario produces a report that is slow to open, expensive to
 * store, and that nobody reads, so the one time an attachment matters it is buried among
 * hundreds that never did.
 *
 * <p>These are ordinary Cucumber hooks, not an Allure listener. Everything else about the
 * integration is configuration — the adapter is named in {@code cucumber.plugin} and nothing
 * post-processes the results.
 *
 * <p>This package is listed on the api and web suites' glue paths and on no other. Each hook is
 * tag-scoped as well, so the api suite never reaches for a browser that does not exist.
 */
public class AllureAttachments {

    @After(value = "@web", order = 100)
    public void attachBrowserEvidenceOnFailure(Scenario scenario) {
        // Cucumber runs @After hooks highest order first, so order = 100 puts this ahead of
        // the driver quit at order = 1. Getting that backwards is silent: the browser closes,
        // this hook finds no driver, and the report simply has no screenshot in it.
        if (!scenario.isFailed() || !DriverProvider.isStarted()) {
            return;
        }
        WebDriver driver = DriverProvider.get();

        if (driver instanceof TakesScreenshot camera) {
            Allure.addAttachment(
                    "Screenshot at failure",
                    "image/png",
                    new ByteArrayInputStream(camera.getScreenshotAs(OutputType.BYTES)),
                    ".png");
        }
        // The page as the browser had it, which answers the question a screenshot cannot: what
        // the locator was actually looking at.
        Allure.addAttachment("Page source at failure", "text/html", driver.getPageSource(), ".html");
        Allure.addAttachment("URL at failure", driver.getCurrentUrl());
    }

    @After("@api or @web")
    public void attachHttpExchangeOnFailure(Scenario scenario) {
        String exchange = ApiExchangeLog.text();
        try {
            if (scenario.isFailed() && !exchange.isBlank()) {
                Allure.addAttachment(
                        "HTTP exchange",
                        "text/plain",
                        new ByteArrayInputStream(exchange.getBytes(StandardCharsets.UTF_8)),
                        ".txt");
            }
        } finally {
            // Cleared whatever happened, so a scenario can never inherit the previous one's
            // exchange from a pooled thread.
            ApiExchangeLog.clear();
        }
    }
}
