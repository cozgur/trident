package dev.ozgurcetintas.trident.demo.parabank.fixtures;

import static io.restassured.RestAssured.given;

import dev.ozgurcetintas.trident.api.RequestSpecFactory;
import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * One place that knows how this project talks to ParaBank's REST API.
 *
 * <p>Thin on purpose. The base URI and timeouts come from the framework's
 * {@link RequestSpecFactory}, which reads them from configuration; all this adds is the
 * {@code Accept} header, because ParaBank serves XML unless asked for JSON.
 *
 * <p>It exists because the alternative is the same four lines in every step. It holds no
 * state, so there is nothing for one scenario to leave behind for another.
 */
public final class ParaBankApi {

    private ParaBankApi() {}

    /** A request aimed at the running container, asking for JSON. */
    public static RequestSpecification request() {
        return given().spec(RequestSpecFactory.from(ConfigProvider.get())).accept(ContentType.JSON);
    }
}
