package dev.ozgurcetintas.trident.web;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.core.context.TridentException;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Base class for a page object that knows when it is ready.
 *
 * <p>A subclass says two things and inherits the rest:
 *
 * <pre>{@code
 * public final class OrdersPage extends LoadablePage {
 *
 *     public OrdersPage(WebDriver driver, TridentConfig config) {
 *         super(driver, config);
 *     }
 *
 *     @Override
 *     protected ExpectedCondition<?> loadedCondition() {
 *         return ExpectedConditions.numberOfElementsToBeMoreThan(ORDER_ROWS, 0);
 *     }
 *
 *     @Override
 *     protected String pageName() {
 *         return "Orders";
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Why the condition is not optional.</strong> The common flake in a server-rendered
 * application is not a slow element, it is a page that has arrived and is not finished: the
 * document is complete, the table is in the DOM, and its rows are still being fetched. A test
 * that starts interacting there reads an empty table and reports a missing record. Every
 * accessor below calls {@link #awaitLoaded()} first, so a subclass cannot forget; the check runs
 * once per instance and then costs nothing.
 *
 * <p>Pick a condition that is false on a half-rendered page. "The table exists" is usually true
 * immediately and proves nothing. "The table has rows", "the dropdown has options", "the result
 * panel is visible" are the useful shapes.
 *
 * <p><strong>No locators live here.</strong> This class supplies the waiting; the selectors
 * belong to the consumer's page objects, next to the application they describe.
 */
public abstract class LoadablePage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private boolean loaded;

    /**
     * @param driver the browser this page is displayed in
     * @param config supplies the timeout and the polling interval, so every wait in a suite
     *     agrees with every other and with the API layer's timeouts
     */
    protected LoadablePage(WebDriver driver, TridentConfig config) {
        this.driver = driver;
        this.wait = new WebDriverWait(
                driver, Duration.ofSeconds(config.defaultTimeoutSeconds()), Duration.ofMillis(config.pollingMillis()));
    }

    /**
     * The condition that is true only once this page has finished rendering.
     *
     * @return a condition to wait for, evaluated against the driver
     */
    protected abstract ExpectedCondition<?> loadedCondition();

    /**
     * A name for this page, used in the failure message when the condition never holds.
     *
     * @return a short human-readable name, such as {@code "Accounts overview"}
     */
    protected abstract String pageName();

    /**
     * Blocks until {@link #loadedCondition()} holds. Idempotent: the wait happens once.
     *
     * @throws TridentException if the condition never holds within the configured timeout
     */
    public final void awaitLoaded() {
        if (loaded) {
            return;
        }
        try {
            wait.until(loadedCondition());
        } catch (TimeoutException e) {
            throw new TridentException(
                    "The " + pageName() + " page did not finish loading within the configured timeout. "
                            + "The browser is at " + driver.getCurrentUrl() + ". Either the page is slow, "
                            + "or its loaded condition describes something this page never shows.",
                    e);
        }
        loaded = true;
    }

    /**
     * Navigates to a URL and waits for this page to be ready.
     *
     * @param url the address to open
     */
    protected final void openAt(String url) {
        driver.get(url);
        loaded = false;
        awaitLoaded();
    }

    /**
     * A visible element, waited for explicitly.
     *
     * @param locator how to find it
     * @return the element, once it is displayed
     */
    protected final WebElement visible(By locator) {
        awaitLoaded();
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * An element that is ready to be clicked, waited for explicitly.
     *
     * @param locator how to find it
     * @return the element, once it is displayed and enabled
     */
    protected final WebElement clickable(By locator) {
        awaitLoaded();
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Every matching element, once all of them are displayed.
     *
     * @param locator how to find them
     * @return the matching elements, never empty — the wait fails instead
     */
    protected final List<WebElement> allVisible(By locator) {
        awaitLoaded();
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /**
     * An element used only as an anchor for a relative or structural locator, so it is waited
     * for as present rather than as visible.
     *
     * <p>Some anchors are legitimately invisible — a container, a heading inside a collapsed
     * panel. Interacting with one is a mistake; pointing a relative locator at one is not.
     *
     * @param locator how to find it
     * @return the element, once it is in the DOM
     */
    protected final WebElement anchor(By locator) {
        awaitLoaded();
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * The shared wait, for a condition this class does not wrap.
     *
     * @return a wait configured with the suite's timeout and polling interval
     */
    protected final WebDriverWait waiter() {
        awaitLoaded();
        return wait;
    }

    /**
     * Where the browser currently is. Not guarded, so it can be read while diagnosing a page
     * that did not load.
     *
     * @return the current URL
     */
    protected final String currentUrl() {
        return driver.getCurrentUrl();
    }
}
