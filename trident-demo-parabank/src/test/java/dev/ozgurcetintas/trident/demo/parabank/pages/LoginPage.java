package dev.ozgurcetintas.trident.demo.parabank.pages;

import static org.openqa.selenium.support.locators.RelativeLocator.with;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.web.LoadablePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * ParaBank's landing page, which is also its login form and its login error page.
 *
 * <p>This is the page the locator ladder in
 * {@code docs/adr/0017-locator-strategy-for-an-unmodifiable-ui.md} exists for. The two inputs
 * carry no id, no {@code aria-label}, and their labels are {@code <p><b>Username</b></p>}
 * elements with no {@code for} attribute, so neither input has an accessible name at all. There
 * is nothing on rung 1 to use and nothing stable on rung 4 either.
 */
public final class LoginPage extends LoadablePage {

    /**
     * Rung 1: the text the user reads. Selenium has no by-visible-text locator, so it is
     * written as a relative XPath, anchored inside the login form rather than at the document
     * root. These two elements are anchors for the relative locators below; they are never
     * interacted with.
     */
    private static final By USERNAME_LABEL = By.xpath(".//form[@name='login']//b[normalize-space()='Username']");

    private static final By PASSWORD_LABEL = By.xpath(".//form[@name='login']//b[normalize-space()='Password']");

    /**
     * Rung 1: for {@code <input type="submit">} the {@code value} attribute is the accessible
     * name, so this matches on the words printed on the button.
     */
    private static final By LOG_IN = By.cssSelector("input[type='submit'][value='Log In']");

    /**
     * Rung 4: a semantic class inside the page's static layout container. Verified against the
     * rendered page rather than assumed — see {@link #errorMessage()}.
     */
    private static final By ERROR = By.cssSelector("#rightPanel p.error");

    private final String baseUrl;

    public LoginPage(WebDriver driver, TridentConfig config) {
        super(driver, config);
        this.baseUrl = config.webBaseUrl();
    }

    /**
     * The login form is rendered by the server in one response, so the button being displayed
     * is enough. Nothing on this page arrives later.
     */
    @Override
    protected ExpectedCondition<?> loadedCondition() {
        return ExpectedConditions.visibilityOfElementLocated(LOG_IN);
    }

    @Override
    protected String pageName() {
        return "Login";
    }

    /** Opens the application's landing page. */
    public LoginPage open() {
        openAt(baseUrl + "/parabank/index.htm");
        return this;
    }

    /**
     * Types credentials and submits.
     *
     * <p>Rung 2: each input is found relative to the label above it. {@code below} orders its
     * matches by proximity to the anchor, so the nearest input under "Username" is the username
     * field even though nothing in the markup says so.
     */
    public void logInAs(String username, String password) {
        usernameField().sendKeys(username);
        passwordField().sendKeys(password);
        WebElement submit = clickable(LOG_IN);
        submit.click();

        // The click starts a navigation, and click() returns before it finishes. Without this
        // wait the step returns while the login POST is still in flight, and the next step's
        // driver.get() races it: the browser asks for one page, the login response arrives
        // afterwards, and the scenario ends up somewhere it never navigated to.
        //
        // That is not theoretical. Measured across 24 runs before this line existed, the
        // scenario that opens an account - the only one that navigates somewhere other than
        // where login lands - failed with the browser reporting overview.htm while waiting for
        // a control on openaccount.htm. The transfer scenario hid the same race, because it
        // navigates to overview.htm, which is where the late login response was going anyway.
        //
        // Waiting for the clicked button to go stale is the page-agnostic form of "the
        // navigation completed": it holds for the account overview and for the error page.
        waiter().until(ExpectedConditions.stalenessOf(submit));
    }

    /**
     * The message ParaBank shows when a login is refused.
     *
     * <p>Recorded before it was asserted: a wrong password returns <strong>HTTP 200</strong>
     * and re-renders the landing page with {@code <h1 class="title">Error!</h1>} and
     * {@code <p class="error">The username and password could not be verified.</p>}. The status
     * proves nothing, and the message names neither the username nor the password — which is
     * the right behaviour, since saying which one was wrong would confirm that an account
     * exists.
     *
     * @return the error text as the user sees it
     */
    public String errorMessage() {
        return visible(ERROR).getText();
    }

    private WebElement usernameField() {
        return visible(with(By.tagName("input")).below(anchor(USERNAME_LABEL)));
    }

    private WebElement passwordField() {
        return visible(with(By.tagName("input")).below(anchor(PASSWORD_LABEL)));
    }
}
