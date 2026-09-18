package dev.ozgurcetintas.trident.demo.parabank.pages;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.web.LoadablePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

/** The form that opens a second account, funded from an existing one. */
public final class OpenAccountPage extends LoadablePage {

    /** Rung 4: static ids in the JSP. Neither is generated from an account or a customer. */
    private static final By ACCOUNT_TYPE = By.id("type");

    private static final By FUNDING_ACCOUNT = By.id("fromAccountId");

    private static final By FUNDING_ACCOUNT_OPTIONS = By.cssSelector("#fromAccountId option");

    /** Rung 1: the words on the button are its accessible name. */
    private static final By OPEN = By.cssSelector("input[type='button'][value='Open New Account']");

    private static final By RESULT_PANEL = By.id("openAccountResult");

    private static final By NEW_ACCOUNT_NUMBER = By.id("newAccountId");

    private final String baseUrl;

    public OpenAccountPage(WebDriver driver, TridentConfig config) {
        super(driver, config);
        this.baseUrl = config.webBaseUrl();
    }

    /**
     * The funding dropdown is empty in the served HTML and filled from a second request.
     * Waiting for the form, or for the dropdown to exist, would let a scenario select from an
     * empty list and fail with something that looks like a Selenium bug.
     */
    @Override
    protected ExpectedCondition<?> loadedCondition() {
        return ExpectedConditions.numberOfElementsToBeMoreThan(FUNDING_ACCOUNT_OPTIONS, 0);
    }

    @Override
    protected String pageName() {
        return "Open new account";
    }

    public OpenAccountPage open() {
        openAt(baseUrl + "/parabank/openaccount.htm");
        return this;
    }

    /**
     * Opens a savings account funded from an existing one.
     *
     * @param fundingAccountNumber the account the opening deposit comes from
     * @return the number of the account ParaBank opened
     */
    public String openSavingsFundedFrom(String fundingAccountNumber) {
        new Select(visible(ACCOUNT_TYPE)).selectByVisibleText("SAVINGS");
        new Select(visible(FUNDING_ACCOUNT)).selectByVisibleText(fundingAccountNumber);
        clickable(OPEN).click();

        waiter().until(ExpectedConditions.visibilityOfElementLocated(RESULT_PANEL));
        return visible(NEW_ACCOUNT_NUMBER).getText().trim();
    }
}
