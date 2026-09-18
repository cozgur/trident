package dev.ozgurcetintas.trident.demo.parabank.pages;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.core.context.TridentException;
import dev.ozgurcetintas.trident.web.LoadablePage;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * The accounts table a customer lands on after logging in.
 *
 * <p>This page is why {@link LoadablePage} demands a loaded condition. The server sends the
 * table with an <strong>empty</strong> {@code <tbody>} and jQuery fills it from a second
 * request. A test that started reading as soon as the document was complete would find no
 * accounts and report a missing one — the failure would be in the application's own data, and
 * the cause would be three layers away.
 */
public final class AccountsOverviewPage extends LoadablePage {

    /** Rung 4: a static id in the JSP, not generated from any account. */
    private static final By TABLE_BODY = By.cssSelector("#accountTable tbody");

    private static final By HEADER_CELLS = By.cssSelector("#accountTable thead th");

    private final String baseUrl;

    public AccountsOverviewPage(WebDriver driver, TridentConfig config) {
        super(driver, config);
        this.baseUrl = config.webBaseUrl();
    }

    /**
     * The table is finished when its totals row exists, not when the table does.
     *
     * <p>The rows are appended one at a time and the "Total" row last, so waiting for that word
     * waits for the whole response rather than for the first account of it. "The table exists"
     * is true immediately and proves nothing.
     */
    @Override
    protected ExpectedCondition<?> loadedCondition() {
        return ExpectedConditions.textToBePresentInElementLocated(TABLE_BODY, "Total");
    }

    @Override
    protected String pageName() {
        return "Accounts overview";
    }

    /** Navigates here directly, for a scenario that is already logged in. */
    public AccountsOverviewPage open() {
        openAt(baseUrl + "/parabank/overview.htm");
        return this;
    }

    /**
     * Every account number listed, in the order the page shows them.
     *
     * @return the account numbers as the user reads them
     */
    public List<String> accountNumbers() {
        return allVisible(accountLinks()).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .toList();
    }

    /**
     * The balance shown against one account.
     *
     * <p>Rung 3: a structural XPath anchored on the account's own link, then the cell in the
     * balance column. The column is located by its heading rather than by position: a
     * {@code td[2]} would be a positional index, which the locator policy bans and the scanning
     * test enforces. Reading the heading costs one extra query and survives a column being
     * added, which is exactly the kind of change nobody announces.
     *
     * @param accountNumber the account to read
     * @return the balance, parsed from the currency the page prints
     */
    public BigDecimal balanceOf(String accountNumber) {
        int balanceColumn = indexOfColumnStartingWith("Balance");
        List<WebElement> cells = rowFor(accountNumber).findElements(By.tagName("td"));
        if (balanceColumn >= cells.size()) {
            throw new TridentException("The accounts table has a 'Balance' heading in column " + balanceColumn
                    + " but the row for account " + accountNumber + " has only " + cells.size() + " cells.");
        }
        return parseCurrency(cells.get(balanceColumn).getText());
    }

    /** True when this account appears in the table at all. */
    public boolean lists(String accountNumber) {
        return accountNumbers().contains(accountNumber);
    }

    private WebElement rowFor(String accountNumber) {
        // Anchored on the link carrying the account number, then up to the row that holds it.
        // Not absolute: the expression starts from the table, and the account number comes from
        // the scenario's own data rather than from anything hardcoded here.
        return visible(
                By.xpath(".//table[@id='accountTable']//a[normalize-space()='" + accountNumber + "']/ancestor::tr"));
    }

    private By accountLinks() {
        // Every account cell is a link to that account's activity; the totals row has none,
        // which is what keeps it out of this list.
        return By.cssSelector("#accountTable tbody tr td a");
    }

    private int indexOfColumnStartingWith(String heading) {
        List<WebElement> headers = allVisible(HEADER_CELLS);
        for (int column = 0; column < headers.size(); column++) {
            if (headers.get(column).getText().trim().startsWith(heading)) {
                return column;
            }
        }
        throw new TridentException(
                "The accounts table has no column whose heading starts with '" + heading + "'. Headings found: "
                        + headers.stream().map(WebElement::getText).toList());
    }

    /**
     * Parses what the page prints. ParaBank formats a negative balance as {@code -$12.34},
     * with the sign before the currency symbol, which no standard parser expects.
     */
    private static BigDecimal parseCurrency(String shown) {
        String digits = shown.replace("$", "").replace(",", "").trim();
        try {
            return new BigDecimal(digits);
        } catch (NumberFormatException e) {
            throw new TridentException("Could not read a balance from '" + shown + "'.", e);
        }
    }
}
