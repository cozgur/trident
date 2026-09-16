package dev.ozgurcetintas.trident.api;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts on the built specification rather than on network behaviour, so these tests need no
 * server and no container. {@link SpecificationQuerier} exposes the specification's own view
 * of itself, which is what the factory is responsible for.
 */
class RequestSpecFactoryTest {

    @Test
    @DisplayName("The base URI comes from configuration")
    void baseUriComesFromConfig() {
        RequestSpecification spec = RequestSpecFactory.from(config("http://api.example:8080", 10L));

        assertThat(query(spec).getBaseUri()).isEqualTo("http://api.example:8080");
    }

    @Test
    @DisplayName("The connection and socket timeouts come from configuration")
    void timeoutComesFromConfig() {
        RequestSpecification spec = RequestSpecFactory.from(config("http://api.example", 7L));

        Map<String, ?> params = query(spec).getConfig().getHttpClientConfig().params();
        assertThat(params.get("http.connection.timeout")).isEqualTo(7_000);
        assertThat(params.get("http.socket.timeout")).isEqualTo(7_000);
    }

    @Test
    @DisplayName("A different configuration produces a specification pointing elsewhere")
    void readsConfigPerCall() {
        RequestSpecification first = RequestSpecFactory.from(config("http://first.example", 10L));
        RequestSpecification second = RequestSpecFactory.from(config("http://second.example", 20L));

        // The factory must not cache the configuration it saw first: a container-backed run
        // resolves its base URI only once the container has started.
        assertThat(query(first).getBaseUri()).isEqualTo("http://first.example");
        assertThat(query(second).getBaseUri()).isEqualTo("http://second.example");
        Map<String, ?> secondParams =
                query(second).getConfig().getHttpClientConfig().params();
        assertThat(secondParams.get("http.socket.timeout")).isEqualTo(20_000);
    }

    private static QueryableRequestSpecification query(RequestSpecification spec) {
        return SpecificationQuerier.query(spec);
    }

    /**
     * A hand-written stub rather than a loaded configuration: {@code TridentConfig} declares
     * five methods and nothing else, so implementing it directly keeps these tests independent
     * of the property files shipped in trident-core.
     */
    private static TridentConfig config(String apiBaseUrl, long timeoutSeconds) {
        return new TridentConfig() {

            @Override
            public String env() {
                return "test";
            }

            @Override
            public String webBaseUrl() {
                return "http://web.example";
            }

            @Override
            public String apiBaseUrl() {
                return apiBaseUrl;
            }

            @Override
            public long defaultTimeoutSeconds() {
                return timeoutSeconds;
            }

            @Override
            public long pollingMillis() {
                return 100L;
            }
        };
    }
}
