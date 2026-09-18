package dev.ozgurcetintas.trident.demo.parabank.pages;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.web.LoadablePage;
import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

/** The form that moves money between two of a customer's own accounts. */
public final class TransferFundsPage extends LoadablePage {

    /** Rung 4: static ids in the JSP. */
    private static final By AMOUNT = By.id("amount");

    private static final By FROM_ACCOUNT = By.id("fromAccountId");

    private static final By TO_ACCOUNT = By.id("toAccountId");

    private static final By FROM_ACCOUNT_OPTIONS = By.cssSelector("#fromAccountId option");

    /** Rung 1: the word on the button. */
    private static final By TRANSFER = By.cssSelector("input[type='submit'][value='Transfer']");

    private static final By RESULT_PANEL = By.id("showResult");

    private final String baseUrl;

    public TransferFundsPage(WebDriver driver, TridentConfig config) {
        super(driver, config);
        this.baseUrl = config.webBaseUrl();
    }

    /** Both dropdowns are filled from a second request; the form arrives before the accounts do. */
    @Override
    protected ExpectedCondition<?> loadedCondition() {
        return ExpectedConditions.numberOfElementsToBeMoreThan(FROM_ACCOUNT_OPTIONS, 0);
    }

    @Override
    protected String pageName() {
        return "Transfer funds";
    }

    public TransferFundsPage open() {
        openAt(baseUrl + "/parabank/transfer.htm");
        return this;
    }

    /**
     * Transfers an amount and waits for ParaBank to confirm it.
     *
     * @param amount how much to move
     * @param fromAccountNumber the account to debit
     * @param toAccountNumber the account to credit
     */
    public void transfer(BigDecimal amount, String fromAccountNumber, String toAccountNumber) {
        visible(AMOUNT).sendKeys(amount.toPlainString());
        new Select(visible(FROM_ACCOUNT)).selectByVisibleText(fromAccountNumber);
        new Select(visible(TO_ACCOUNT)).selectByVisibleText(toAccountNumber);
        clickable(TRANSFER).click();
        waiter().until(ExpectedConditions.visibilityOfElementLocated(RESULT_PANEL));
    }

    /**
     * What the confirmation panel says.
     *
     * @return the confirmation text as the user reads it
     */
    public String confirmation() {
        return visible(RESULT_PANEL).getText();
    }
}
