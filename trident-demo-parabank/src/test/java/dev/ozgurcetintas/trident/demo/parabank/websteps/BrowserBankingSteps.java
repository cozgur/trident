package dev.ozgurcetintas.trident.demo.parabank.websteps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.core.context.ScenarioContext;
import dev.ozgurcetintas.trident.demo.parabank.fixtures.CustomerFactory;
import dev.ozgurcetintas.trident.demo.parabank.fixtures.ParaBankCustomer;
import dev.ozgurcetintas.trident.demo.parabank.pages.AccountsOverviewPage;
import dev.ozgurcetintas.trident.demo.parabank.pages.LoginPage;
import dev.ozgurcetintas.trident.demo.parabank.pages.OpenAccountPage;
import dev.ozgurcetintas.trident.demo.parabank.pages.TransferFundsPage;
import dev.ozgurcetintas.trident.web.DriverProvider;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import org.openqa.selenium.WebDriver;

/**
 * Banking through the browser.
 *
 * <p>Not one {@code By} appears here, and none should. Steps say what a customer does; where
 * the fields are is the page objects' business, and keeping that boundary is what lets a
 * locator change without a scenario changing. A test asserts it rather than a reviewer
 * remembering it — see {@code LocatorPolicyTest}.
 *
 * <p>The fixtures are REST. A scenario about transferring money should not spend four steps
 * registering a customer through a form, and every second of that would be charged to the web
 * suite's runtime. Only the assertions are about the UI.
 */
public class BrowserBankingSteps {

    private static final String SECOND_ACCOUNT = "secondAccountNumber";
    private static final String BALANCE_BEFORE_FROM = "balanceBeforeFrom";
    private static final String BALANCE_BEFORE_TO = "balanceBeforeTo";
    private static final String NEW_ACCOUNT = "newAccountNumber";
    private static final String LOGIN_PAGE = "loginPage";

    private final ScenarioContext context;
    private final CustomerFactory customers;

    public BrowserBankingSteps(ScenarioContext context, CustomerFactory customers) {
        this.context = context;
        this.customers = customers;
    }

    @Given("a registered customer")
    public void aRegisteredCustomer() {
        customers.createCustomer();
    }

    @Given("a registered customer with two accounts")
    public void aRegisteredCustomerWithTwoAccounts() {
        ParaBankCustomer customer = customers.createCustomer();
        context.put(SECOND_ACCOUNT, String.valueOf(customers.openSecondAccount(customer)));
    }

    @Given("they are logged in through the browser")
    @When("they log in through the browser")
    public void theyLogInThroughTheBrowser() {
        ParaBankCustomer customer = customer();
        loginPage().open().logInAs(customer.username(), customer.password());
    }

    @When("they log in through the browser with the wrong password")
    public void theyLogInWithTheWrongPassword() {
        LoginPage page = loginPage();
        context.put(LOGIN_PAGE, page);
        page.open().logInAs(customer().username(), "not-" + customer().password());
    }

    @When("they open a savings account funded from their first account")
    public void theyOpenASavingsAccount() {
        String opened = new OpenAccountPage(driver(), config())
                .open()
                .openSavingsFundedFrom(String.valueOf(customer().initialAccountId()));
        context.put(NEW_ACCOUNT, opened);
    }

    @When("they transfer ${double} from the first account to the second")
    public void theyTransfer(Double amount) {
        String from = String.valueOf(customer().initialAccountId());
        String to = context.get(SECOND_ACCOUNT, String.class);

        AccountsOverviewPage before = new AccountsOverviewPage(driver(), config()).open();
        context.put(BALANCE_BEFORE_FROM, before.balanceOf(from));
        context.put(BALANCE_BEFORE_TO, before.balanceOf(to));

        new TransferFundsPage(driver(), config()).open().transfer(BigDecimal.valueOf(amount), from, to);
    }

    @Then("they see their own account in the overview")
    public void theySeeTheirOwnAccount() {
        AccountsOverviewPage overview = new AccountsOverviewPage(driver(), config());
        assertThat(overview.accountNumbers())
                .containsExactly(String.valueOf(customer().initialAccountId()));
    }

    @Then("the new account appears in the overview")
    public void theNewAccountAppearsInTheOverview() {
        String opened = context.get(NEW_ACCOUNT, String.class);
        AccountsOverviewPage overview = new AccountsOverviewPage(driver(), config()).open();

        assertThat(overview.lists(opened))
                .as("account %s, just opened, should be listed in the overview", opened)
                .isTrue();
        assertThat(overview.accountNumbers())
                .containsExactlyInAnyOrder(String.valueOf(customer().initialAccountId()), opened);
    }

    @Then("both balances have moved by ${double}")
    public void bothBalancesHaveMoved(Double amount) {
        BigDecimal moved = BigDecimal.valueOf(amount);
        String from = String.valueOf(customer().initialAccountId());
        String to = context.get(SECOND_ACCOUNT, String.class);

        AccountsOverviewPage after = new AccountsOverviewPage(driver(), config()).open();

        assertThat(after.balanceOf(from))
                .as("the funding account should be %s lighter", moved)
                .isEqualByComparingTo(
                        context.get(BALANCE_BEFORE_FROM, BigDecimal.class).subtract(moved));
        assertThat(after.balanceOf(to))
                .as("the receiving account should be %s heavier", moved)
                .isEqualByComparingTo(
                        context.get(BALANCE_BEFORE_TO, BigDecimal.class).add(moved));
    }

    @Then("the page says the username and password could not be verified")
    public void thePageSaysCredentialsCouldNotBeVerified() {
        // Recorded before it was asserted. ParaBank answers a refused login with HTTP 200 and
        // re-renders the landing page carrying this sentence, so the status says nothing and
        // the words are the only evidence. The message deliberately names neither field.
        assertThat(context.get(LOGIN_PAGE, LoginPage.class).errorMessage())
                .isEqualTo("The username and password could not be verified.");
    }

    private ParaBankCustomer customer() {
        return context.get(CustomerFactory.CONTEXT_KEY, ParaBankCustomer.class);
    }

    private LoginPage loginPage() {
        return new LoginPage(driver(), config());
    }

    private static WebDriver driver() {
        return DriverProvider.get();
    }

    private static TridentConfig config() {
        return ConfigProvider.get();
    }
}
