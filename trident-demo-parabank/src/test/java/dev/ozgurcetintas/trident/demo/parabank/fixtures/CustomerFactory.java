package dev.ozgurcetintas.trident.demo.parabank.fixtures;

import static org.hamcrest.Matchers.containsString;

import dev.ozgurcetintas.trident.core.context.ScenarioContext;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;
import java.util.UUID;

/**
 * Creates a customer that belongs to one scenario and to nothing else.
 *
 * <p><strong>Why this posts an HTML form.</strong> ParaBank's REST API can read customers and
 * open accounts, but it exposes no endpoint that creates one — the only way in is the
 * registration page. So this is fixture setup performed through the UI's form, and it is never
 * the subject under test: no scenario asserts on the registration response, and if ParaBank
 * added a REST endpoint tomorrow this class would change and no scenario would.
 *
 * <p>Registration needs a session. A bare POST fails with
 * {@code Expected session attribute 'customerForm'}, because the controller populates the form
 * object on the GET. The {@link SessionFilter} carries the JSESSIONID from one to the other.
 *
 * <p>Picocontainer builds this once per scenario, along with the {@link ScenarioContext} it
 * writes to, so no customer is ever shared between scenarios.
 */
public class CustomerFactory {

    /** Key the created customer is stored under, for steps that need it later. */
    public static final String CONTEXT_KEY = "customer";

    /** ParaBank's account types are ordinals, not names: 0 is CHECKING and 1 is SAVINGS. */
    private static final int SAVINGS_ACCOUNT_TYPE = 1;

    private final ScenarioContext context;

    public CustomerFactory(ScenarioContext context) {
        this.context = context;
    }

    /**
     * Registers a new customer and records it in the scenario context.
     *
     * @return the customer, its credentials and the account ParaBank opened with it
     */
    public ParaBankCustomer createCustomer() {
        String username = uniqueUsername();
        String password = "Trident" + username.substring(username.length() - 8);

        SessionFilter session = new SessionFilter();
        ParaBankApi.form(session).when().get("/parabank/register.htm").then().statusCode(200);

        ParaBankApi.form(session)
                .formParam("customer.firstName", "Trident")
                .formParam("customer.lastName", "Fixture")
                .formParam("customer.address.street", "1 Test Street")
                .formParam("customer.address.city", "Testville")
                .formParam("customer.address.state", "TS")
                .formParam("customer.address.zipCode", "12345")
                .formParam("customer.phoneNumber", "5555555555")
                .formParam("customer.ssn", "123-45-6789")
                .formParam("customer.username", username)
                .formParam("customer.password", password)
                .formParam("repeatedPassword", password)
                .when()
                .post("/parabank/register.htm")
                .then()
                .statusCode(200)
                // ParaBank answers 200 whether it registered the customer or re-rendered the
                // form with a validation error, so the status proves nothing by itself. Assert
                // on the page's own words, or a rejected registration surfaces three calls
                // later as an unexplained 400 from login.
                .body(containsString("Your account was created successfully"));

        JsonPath customer = ParaBankApi.request()
                .when()
                .get("/parabank/services/bank/login/{username}/{password}", username, password)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        int customerId = customer.getInt("id");

        int accountId = ParaBankApi.request()
                .when()
                .get("/parabank/services/bank/customers/{id}/accounts", customerId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("[0].id");

        ParaBankCustomer created = new ParaBankCustomer(username, password, customerId, accountId);
        context.put(CONTEXT_KEY, created);
        return created;
    }

    /**
     * Opens a second account for a customer this scenario already created.
     *
     * <p>Fixture setup, over REST, for the same reason the registration form is: a scenario
     * about transferring money needs two accounts to exist and should not spend its assertions
     * proving that opening one works. The scenario that <em>is</em> about opening an account
     * does it through the browser instead.
     *
     * <p>ParaBank moves an opening deposit of $100 out of the funding account. The response
     * body reports the new account's balance as 0 and the accounts list then reports 100, so
     * the balance is read from the list rather than from this response.
     *
     * @param customer the customer to open it for
     * @return the new account's number
     */
    public int openSecondAccount(ParaBankCustomer customer) {
        return ParaBankApi.request()
                .queryParam("customerId", customer.customerId())
                .queryParam("newAccountType", SAVINGS_ACCOUNT_TYPE)
                .queryParam("fromAccountId", customer.initialAccountId())
                .when()
                .post("/parabank/services/bank/createAccount")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    /**
     * A name no other scenario will produce.
     *
     * <p>Random, not a counter: a counter is unique only within one JVM, and would start
     * colliding the moment two runs shared a ParaBank instance or the suite ran in parallel.
     *
     * <p>Nineteen characters, because ParaBank's username column holds twenty. Measured on this
     * image: a twenty-character name registers, twenty-one is rejected — and rejected with
     * <em>"This username already exists"</em> on a database where it certainly does not, which
     * is a long way from the real cause. Twelve hex characters is 48 bits of randomness, ample
     * within one suite.
     */
    private static String uniqueUsername() {
        return "trident" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
