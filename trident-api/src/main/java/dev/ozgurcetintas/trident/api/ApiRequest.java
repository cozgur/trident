package dev.ozgurcetintas.trident.api;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Sends a request and returns the response whatever the status was.
 *
 * <p><strong>Why this exists.</strong> REST Assured, in every configuration measured, surfaces
 * a non-2xx response by throwing {@code HttpResponseException} from the request itself, so the
 * response never reaches an assertion. That was checked across two major versions, four request
 * shapes including {@code .then().statusCode(400)}, and against a five-line Python server that
 * returned an ordinary 400 — it is not specific to any application under test. The reasoning is
 * in {@code docs/adr/0012}.
 *
 * <p>So a scenario that asserts a rejection — a 401 for a missing token, a 403, a 404 — cannot
 * use {@link RequestSpecFactory}. The first project to need it wrote a JDK client inline. The
 * second did the same, in a different repository, against a different application. That is the
 * point at which it belongs in the framework rather than in everyone's step definitions.
 *
 * <p>Use {@link RequestSpecFactory} for everything else. This class exists for the narrow case
 * of asserting on a response REST Assured will not hand back, and it is deliberately small: no
 * body serialisation, no fluent builder, no response parsing. If you need those, you are on the
 * success path and should be using the specification.
 */
public final class ApiRequest {

    private ApiRequest() {}

    /**
     * Sends a request with no body and returns whatever came back.
     *
     * @param config supplies the base URI and the timeout
     * @param method the HTTP method, for example {@code GET}
     * @param path the path to append to the configured base URI, starting with {@code /}
     * @param headers headers to send; may be empty
     * @return the status and body, never {@code null}
     * @throws IllegalStateException if the request could not be sent at all
     */
    public static ApiResponse send(TridentConfig config, String method, String path, Map<String, String> headers) {
        Duration timeout = Duration.ofSeconds(config.defaultTimeoutSeconds());
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(config.apiBaseUrl() + path))
                .timeout(timeout)
                .method(method, HttpRequest.BodyPublishers.noBody());
        headers.forEach(request::header);

        try {
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .build()
                    .send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Could not reach " + config.apiBaseUrl() + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while requesting " + path, e);
        }
    }
}
