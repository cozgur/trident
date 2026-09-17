package dev.ozgurcetintas.trident.demo.parabank.container;

import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts one ParaBank container for the Failsafe JVM run and stops it when the run ends.
 *
 * <p>Scope is the suite, not the scenario. A container per scenario would be cleaner in
 * principle and unusable in practice: this image takes several seconds to become ready, so
 * five scenarios would pay that cost five times over. Isolation comes from scenario-owned
 * data instead — each scenario creates its own customer and accounts and reads nothing another
 * created.
 *
 * <p>The container reference is static, and it is the only static mutable state Trident
 * permits. Everything a scenario touches — the context, customers, account ids, responses — is
 * per-scenario and instance-scoped.
 */
public class ParaBankLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(ParaBankLifecycle.class);

    /** Key the container's address is published under, matching TridentConfig's api.base.url. */
    private static final String API_BASE_URL_PROPERTY = "api.base.url";

    /** The REST endpoint the @api scenarios use, asked once before any scenario starts. */
    private static final String REST_READINESS_PATH = "/parabank/services/bank/accounts/12345";

    private static final Duration REST_READINESS_TIMEOUT = Duration.ofMinutes(1);

    private static ParaBankContainer container;

    @BeforeAll
    public static void startParaBank() {
        Instant start = Instant.now();
        container = new ParaBankContainer();
        container.start();
        Duration startup = Duration.between(start, Instant.now());

        // Published as a system property, which is the highest layer of the existing
        // configuration precedence, and set here because @BeforeAll runs before any step can
        // touch ConfigProvider.get() — which resolves once and caches for the life of the JVM.
        // The alternative, a setter on ConfigProvider, would put knowledge of a container into
        // a published framework module.
        System.setProperty(API_BASE_URL_PROPERTY, container.baseUri());

        awaitRestApi(container.baseUri());

        LOG.info("ParaBank started in {} ms at {}", startup.toMillis(), container.baseUri());
    }

    @AfterAll
    public static void stopParaBank() {
        if (container != null) {
            container.stop();
            LOG.info("ParaBank stopped");
        }
    }

    /**
     * Waits for the REST API itself, after the home page has triggered initialisation.
     *
     * <p>The container's wait strategy asks for {@code /parabank/index.htm}, which is what
     * makes ParaBank build its schema — see
     * {@code docs/adr/0011-readiness-probe-triggers-schema-init.md}. That the page answered is
     * not proof that the REST layer will: measured, REST followed 0.1s later, but 0.1s is an
     * observation, not a guarantee. This closes the gap by asking the API the question
     * directly, so a scenario never starts against a container that cannot serve it.
     */
    private static void awaitRestApi(String baseUri) {
        Duration timeout = Duration.ofSeconds(ConfigProvider.get().defaultTimeoutSeconds());
        Instant deadline = Instant.now().plus(REST_READINESS_TIMEOUT);
        int lastStatus = -1;

        while (Instant.now().isBefore(deadline)) {
            try {
                HttpResponse<String> response = HttpClient.newBuilder()
                        .connectTimeout(timeout)
                        .build()
                        .send(
                                HttpRequest.newBuilder(URI.create(baseUri + REST_READINESS_PATH))
                                        .timeout(timeout)
                                        .header("Accept", "application/json")
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());
                lastStatus = response.statusCode();
                if (lastStatus == 200) {
                    return;
                }
            } catch (IOException e) {
                lastStatus = -1;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for ParaBank's REST API", e);
            }
            sleepBriefly();
        }

        throw new IllegalStateException("ParaBank served its home page but its REST API never became ready within "
                + REST_READINESS_TIMEOUT.toSeconds() + "s (last status from " + REST_READINESS_PATH + ": " + lastStatus
                + "). The container is up; check its logs for a failed schema initialisation.");
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(ConfigProvider.get().pollingMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for ParaBank's REST API", e);
        }
    }

    /**
     * Fails once, clearly, if the container is not up.
     *
     * <p>This guards against the container dying mid-run. It does not, and cannot, prove the
     * REST layer answers — that is established once in {@link #awaitRestApi} before any
     * scenario starts, which is the right place for it.
     *
     * <p>Without this hook the first scenario reports a connection refused from somewhere
     * inside REST Assured, and every scenario after it repeats the same error, so the log is
     * long and the cause is not in it.
     */
    @Before("@api")
    public void requireParaBank() {
        if (container == null || !container.isRunning()) {
            throw new IllegalStateException("ParaBank is not running, so no @api scenario can pass. "
                    + "The suite starts it in @BeforeAll; check the container logs above for why "
                    + "it failed to start, and that Docker is available (docker info).");
        }
    }
}
