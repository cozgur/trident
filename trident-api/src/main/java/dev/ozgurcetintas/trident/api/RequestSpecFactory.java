package dev.ozgurcetintas.trident.api;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.concurrent.TimeUnit;

/**
 * Builds REST Assured request specifications from Trident's configuration.
 *
 * <p>Everything the specification carries comes from {@link TridentConfig}: the base URI, and
 * the connection and socket timeouts. Nothing here knows which application is under test, and
 * no path is hardcoded — callers append their own.
 *
 * <p>Authentication is deliberately absent. Schemes differ per application, and a factory that
 * guessed one would have to be worked around by every consumer whose API disagreed.
 *
 * <p>The specification is built per call rather than cached, so a configuration change is
 * picked up by the next request rather than by the next JVM.
 */
public final class RequestSpecFactory {

    private RequestSpecFactory() {}

    /**
     * Builds a specification pointed at the configured API base URI.
     *
     * @param config the configuration to read the base URI and timeout from
     * @return a new specification, never {@code null}
     */
    public static RequestSpecification from(TridentConfig config) {
        int timeoutMillis = (int) TimeUnit.SECONDS.toMillis(config.defaultTimeoutSeconds());

        RestAssuredConfig restAssuredConfig = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeoutMillis)
                        .setParam("http.socket.timeout", timeoutMillis))
                // Request and response are logged only when an expectation fails, so a passing
                // suite stays readable and a failing one carries the exchange that explains it.
                // This is set on the specification rather than through
                // RestAssured.enableLoggingOfRequestAndResponseIfValidationFails, which mutates
                // global state shared by every specification in the JVM.
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL));

        return new RequestSpecBuilder()
                .setBaseUri(config.apiBaseUrl())
                .setContentType(ContentType.JSON)
                .setConfig(restAssuredConfig)
                .build();
    }
}
