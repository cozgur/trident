package dev.ozgurcetintas.trident.demo.parabank.fixtures;

import static io.restassured.RestAssured.given;

import dev.ozgurcetintas.trident.api.RequestSpecFactory;
import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * One place that knows how this project talks to ParaBank.
 *
 * <p>Thin on purpose. Both methods start from the framework's {@link RequestSpecFactory}, so
 * every request in the suite carries the base URI and the connection and socket timeouts that
 * configuration defines. Requests built with a bare {@code given()} would silently opt out of
 * both, and a hung server would stall the suite instead of failing it.
 *
 * <p>Holds no state, so there is nothing for one scenario to leave behind for another.
 */
public final class ParaBankApi {

    private ParaBankApi() {}

    /** A JSON request against the REST API — ParaBank serves XML unless asked for JSON. */
    public static RequestSpecification request() {
        return recorded(
                given().spec(RequestSpecFactory.from(ConfigProvider.get())).accept(ContentType.JSON));
    }

    /**
     * Records the exchange into memory so that a failing scenario can attach it.
     *
     * <p>REST Assured's own logging filters, not a filter written here. They write to a
     * thread-scoped buffer rather than to the console: a passing scenario throws the buffer
     * away and attaches nothing, and a failing one attaches exactly the requests that led to
     * it. See {@link ApiExchangeLog}.
     */
    private static RequestSpecification recorded(RequestSpecification spec) {
        return spec.filter(new RequestLoggingFilter(ApiExchangeLog.stream()))
                .filter(new ResponseLoggingFilter(ApiExchangeLog.stream()));
    }

    /**
     * A form request against the HTML UI, used only to register a customer.
     *
     * <p>Same base URI and timeouts as {@link #request()}, with the content type the
     * registration form needs. The session filter carries the JSESSIONID that ParaBank's
     * registration controller requires between the GET and the POST.
     */
    public static RequestSpecification form(SessionFilter session) {
        return recorded(given().spec(RequestSpecFactory.from(ConfigProvider.get()))
                .filter(session)
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC));
    }
}
