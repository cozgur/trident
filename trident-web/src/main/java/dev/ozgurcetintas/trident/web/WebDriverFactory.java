package dev.ozgurcetintas.trident.web;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.core.context.TridentException;
import java.time.Duration;
import java.util.Locale;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Builds a browser from Trident's configuration.
 *
 * <p>Everything the driver is given comes from {@link TridentConfig}: which browser, whether it
 * is headless, and the page-load and script timeouts. Nothing here knows which application is
 * under test.
 *
 * <p><strong>No driver manager.</strong> Selenium 4.6 and later ship Selenium Manager, which
 * resolves both the browser binary and its driver. Adding WebDriverManager on top would be a
 * second resolver competing with the built-in one, and a second thing to keep current.
 *
 * <p><strong>No implicit wait, deliberately.</strong> Calling
 * {@code manage().timeouts().implicitlyWait(...)} would make every {@code findElement} in the
 * suite block for that duration, and mixing it with the explicit waits in {@link LoadablePage}
 * produces wait times that are neither of the two and are documented by Selenium as undefined.
 * Waiting is explicit, and it happens in one place.
 *
 * <p>Page-load and script timeouts are set, and they are not implicit waits: they bound how
 * long a navigation or an injected script may take before the driver gives up. Without them a
 * hung server stalls the run instead of failing it — the same reasoning that puts socket
 * timeouts on every HTTP request in {@code trident-api}.
 */
public final class WebDriverFactory {

    private WebDriverFactory() {}

    /**
     * Builds a driver for the configured browser.
     *
     * @param config the configuration to read the browser, headless flag and timeout from
     * @return a started driver, which the caller owns and must quit
     * @throws TridentException if {@code browser} names something this factory cannot build
     */
    public static WebDriver create(TridentConfig config) {
        String requested = config.browser();
        if (requested == null || requested.isBlank()) {
            throw new TridentException("No browser configured. Set the 'browser' property or the "
                    + "BROWSER environment variable to one of: chrome, firefox.");
        }

        // Locale.ROOT, not the default locale. Lowercasing without one is the defect recorded in
        // ADR 0016: under Turkish rules "CHROME" lowercases to "chrome" with a dotless i, which
        // then matches nothing and reports a browser the user plainly did ask for.
        WebDriver driver =
                switch (requested.trim().toLowerCase(Locale.ROOT)) {
                    case "chrome" -> new ChromeDriver(chromeOptions(config));
                    case "firefox" -> new FirefoxDriver(firefoxOptions(config));
                    default ->
                        throw new TridentException(
                                "Unknown browser '" + requested + "'. Trident builds: chrome, firefox.");
                };

        Duration timeout = Duration.ofSeconds(config.defaultTimeoutSeconds());
        driver.manage().timeouts().pageLoadTimeout(timeout).scriptTimeout(timeout);
        return driver;
    }

    private static ChromeOptions chromeOptions(TridentConfig config) {
        ChromeOptions options = new ChromeOptions();
        if (config.headless()) {
            options.addArguments("--headless=new");
        }
        // A fixed window size, so that a viewport-dependent layout renders the same way on a
        // developer's laptop and on a CI runner with no display at all.
        options.addArguments("--window-size=1920,1080");
        return options;
    }

    private static FirefoxOptions firefoxOptions(TridentConfig config) {
        FirefoxOptions options = new FirefoxOptions();
        if (config.headless()) {
            options.addArguments("-headless");
        }
        options.addArguments("--width=1920", "--height=1080");
        return options;
    }
}
