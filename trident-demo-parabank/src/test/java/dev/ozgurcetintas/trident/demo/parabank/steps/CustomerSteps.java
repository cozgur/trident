package dev.ozgurcetintas.trident.demo.parabank.steps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ozgurcetintas.trident.core.context.ScenarioContext;
import dev.ozgurcetintas.trident.demo.parabank.fixtures.CustomerFactory;
import dev.ozgurcetintas.trident.demo.parabank.fixtures.ParaBankApi;
import dev.ozgurcetintas.trident.demo.parabank.fixtures.ParaBankCustomer;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;

/**
 * Steps covering the customer a scenario creates for itself.
 *
 * <p>Both the context and the factory arrive by constructor injection, so each scenario gets
 * its own pair and nothing is shared between them.
 */
public class CustomerSteps {

    private final ScenarioContext context;
    private final CustomerFactory customers;

    public CustomerSteps(ScenarioContext context, CustomerFactory customers) {
        this.context = context;
        this.customers = customers;
    }

    @Given("this scenario has no customer yet")
    public void thisScenarioHasNoCustomerYet() {
        // If a previous scenario's data reached this one, it would be here.
        assertThat(context.contains(CustomerFactory.CONTEXT_KEY)).isFalse();
    }

    @When("I register a new customer")
    public void iRegisterANewCustomer() {
        customers.createCustomer();
    }

    @Then("the customer can log in with their own credentials")
    public void theCustomerCanLogIn() {
        ParaBankCustomer customer = context.get(CustomerFactory.CONTEXT_KEY, ParaBankCustomer.class);

        int loggedIn = ParaBankApi.request()
                .when()
                .get("/parabank/services/bank/login/{u}/{p}", customer.username(), customer.password())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("id");

        assertThat(loggedIn).isEqualTo(customer.customerId());
    }

    @Then("the customer owns exactly one account, and it is theirs")
    public void theCustomerOwnsExactlyOneAccount() {
        ParaBankCustomer customer = context.get(CustomerFactory.CONTEXT_KEY, ParaBankCustomer.class);

        List<Integer> owners = ParaBankApi.request()
                .when()
                .get("/parabank/services/bank/customers/{id}/accounts", customer.customerId())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("customerId", Integer.class);

        // Exactly one account, and no account belonging to a customer another scenario made.
        assertThat(owners).containsExactly(customer.customerId());
    }
}
