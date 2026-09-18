package dev.ozgurcetintas.trident.demo.parabank.steps;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import dev.ozgurcetintas.trident.api.ApiRequest;
import dev.ozgurcetintas.trident.api.ApiResponse;
import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import dev.ozgurcetintas.trident.core.context.ScenarioContext;
import dev.ozgurcetintas.trident.demo.parabank.fixtures.CustomerFactory;
import dev.ozgurcetintas.trident.demo.parabank.fixtures.ParaBankApi;
import dev.ozgurcetintas.trident.demo.parabank.fixtures.ParaBankCustomer;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Banking steps, all operating on accounts this scenario's own customer owns.
 *
 * <p>Nothing here reads a value another scenario could have changed: every balance is compared
 * against the same account's balance read earlier in the same scenario, and every account id
 * was created by this scenario.
 */
public class BankingSteps {

    private static final String SECOND_ACCOUNT = "secondAccountId";
    private static final String BALANCE_BEFORE_FIRST = "balanceBeforeFirst";
    private static final String BALANCE_BEFORE_SECOND = "balanceBeforeSecond";

    private final ScenarioContext context;
    private final CustomerFactory customers;

    public BankingSteps(ScenarioContext context, CustomerFactory customers) {
        this.context = context;
        this.customers = customers;
    }

    @Given("I have registered a customer")
    public void iHaveRegisteredACustomer() {
        customers.createCustomer();
    }

    @Given("I have opened a second account funded from my first")
    public void iHaveOpenedASecondAccount() {
        ParaBankCustomer me = customer();

        int newAccountId = ParaBankApi.request()
                .queryParam("customerId", me.customerId())
                .queryParam("newAccountType", 1)
                .queryParam("fromAccountId", me.initialAccountId())
                .when()
                .post("/parabank/services/bank/createAccount")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("id");

        context.put(SECOND_ACCOUNT, newAccountId);
    }

    @When("I log in with my own credentials")
    public void iLogInWithMyOwnCredentials() {
        ParaBankCustomer me = customer();
        context.put("loginResponse", login(me.username(), me.password()));
    }

    @When("I log in with the wrong password")
    public void iLogInWithTheWrongPassword() {
        // REST Assured throws on a non-2xx instead of returning it, so a rejection cannot be
        // asserted through the request specification. ApiRequest is the framework's answer to
        // that; see docs/adr/0012. This step used to carry its own JDK client, and the showcase
        // grew a second copy before it moved here.
        ParaBankCustomer me = customer();
        ApiResponse rejected = ApiRequest.send(
                ConfigProvider.get(),
                "GET",
                "/parabank/services/bank/login/" + me.username() + "/not-the-password",
                Map.of("Accept", "application/json"));
        context.put("rejected", rejected);
    }

    @When("I transfer {int} from my first account to my second")
    public void iTransfer(int amount) {
        ParaBankCustomer me = customer();
        context.put(BALANCE_BEFORE_FIRST, balanceOf(me.initialAccountId()));
        context.put(BALANCE_BEFORE_SECOND, balanceOf(secondAccountId()));

        context.put(
                "transferResponse",
                ParaBankApi.request()
                        .queryParam("fromAccountId", me.initialAccountId())
                        .queryParam("toAccountId", secondAccountId())
                        .queryParam("amount", amount)
                        .when()
                        .post("/parabank/services/bank/transfer")
                        .then()
                        .extract()
                        .response());
    }

    @Then("the API returns my customer record")
    public void theApiReturnsMyCustomerRecord() {
        ParaBankCustomer me = customer();
        Response response = context.get("loginResponse", Response.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getInt("id")).isEqualTo(me.customerId());
        assertThat(response.jsonPath().getString("firstName")).isEqualTo("Trident");
    }

    @Then("the API rejects the login")
    public void theApiRejectsTheLogin() {
        ApiResponse rejected = context.get("rejected", ApiResponse.class);

        // ParaBank gets this one right: a real 400 with a plain-text reason, unlike the
        // registration form and the overdraft below.
        assertThat(rejected.statusCode()).isEqualTo(400);
        assertThat(rejected.body()).contains("Invalid username and/or password");
    }

    @Then("the new account belongs to me and is not my first")
    public void theNewAccountBelongsToMe() {
        ParaBankCustomer me = customer();

        int owner = ParaBankApi.request()
                .when()
                .get("/parabank/services/bank/accounts/{id}", secondAccountId())
                .then()
                .statusCode(200)
                // Structure, not values: the two assertions below cover the values this
                // scenario cares about, and the schema covers the fields it does not read —
                // so a field disappearing or changing type fails here rather than as a null
                // somewhere later.
                .body(matchesJsonSchemaInClasspath("schemas/account.json"))
                .extract()
                .jsonPath()
                .getInt("customerId");

        assertThat(owner).isEqualTo(me.customerId());
        assertThat(secondAccountId()).isNotEqualTo(me.initialAccountId());
    }

    @Then("{int} has moved from my first account to my second")
    public void theMoneyHasMoved(int amount) {
        BigDecimal moved = BigDecimal.valueOf(amount);

        assertThat(context.get("transferResponse", Response.class).statusCode()).isEqualTo(200);
        assertThat(balanceOf(customer().initialAccountId()))
                .isEqualByComparingTo(
                        context.get(BALANCE_BEFORE_FIRST, BigDecimal.class).subtract(moved));
        assertThat(balanceOf(secondAccountId()))
                .isEqualByComparingTo(
                        context.get(BALANCE_BEFORE_SECOND, BigDecimal.class).add(moved));
    }

    @Then("my second account's transactions include a credit of {int}")
    public void transactionsIncludeACredit(int amount) {
        List<Float> credits = ParaBankApi.request()
                .when()
                .get("/parabank/services/bank/accounts/{id}/transactions", secondAccountId())
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/transactions.json"))
                .extract()
                .jsonPath()
                .getList("findAll { it.type == 'Credit' }.amount", Float.class);

        assertThat(credits).contains((float) amount);
    }

    @Then("ParaBank accepts it and overdraws my account")
    public void paraBankOverdrawsTheAccount() {
        // Asserting the defect, not the banking rule. ParaBank answers 200 with
        // "Successfully transferred ..." for a transfer far beyond the balance, and leaves the
        // source account deeply negative - measured at -9,999,684.50 from a balance of 315.50.
        // A real bank rejects this. The assertion below is what the application does, so the
        // suite stays green and honest; if ParaBank is ever fixed, this scenario fails and the
        // fix is to rewrite it as a rejection, not to relax it.
        // No upstream issue is filed: ParaBank is a demo target we do not own, and chasing its
        // defects is not this project's job.
        assertThat(context.get("transferResponse", Response.class).statusCode()).isEqualTo(200);
        assertThat(balanceOf(customer().initialAccountId())).isNegative();
    }

    /**
     * Returns the response whatever the status.
     *
     * <p>{@code extract().response()} rather than a bare {@code get()}: without a response
     * specification REST Assured routes a 4xx through its default failure handler and throws
     * {@code HttpResponseException} instead of handing back the response, so the
     * wrong-password scenario could never assert on the rejection it exists to check.
     */
    private Response login(String username, String password) {
        return ParaBankApi.request()
                .when()
                .get("/parabank/services/bank/login/{username}/{password}", username, password)
                .then()
                .extract()
                .response();
    }

    private BigDecimal balanceOf(int accountId) {
        return ParaBankApi.request()
                .when()
                .get("/parabank/services/bank/accounts/{id}", accountId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getObject("balance", BigDecimal.class);
    }

    private ParaBankCustomer customer() {
        return context.get(CustomerFactory.CONTEXT_KEY, ParaBankCustomer.class);
    }

    private int secondAccountId() {
        return context.get(SECOND_ACCOUNT, Integer.class);
    }
}
