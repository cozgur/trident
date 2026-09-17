package dev.ozgurcetintas.trident.demo.parabank.container;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * The ParaBank application under test, in a container.
 *
 * <p>Lives in the demo module and nowhere else: no framework module knows this class, or
 * ParaBank, or Testcontainers exists. CI enforces that with a grep over the published modules.
 */
final class ParaBankContainer extends GenericContainer<ParaBankContainer> {

    /**
     * Pinned by digest rather than by {@code :latest}, so the application under test cannot
     * change underneath the suite. Obtained on 2026-09-17 with:
     *
     * <pre>{@code
     * docker buildx imagetools inspect parasoft/parabank
     * }</pre>
     *
     * <p>This is the multi-arch index digest, not a per-platform manifest: it resolves to
     * linux/arm64 on an Apple Silicon laptop and linux/amd64 on a CI runner, so one pin works
     * in both places. Pinning it also pins ParaBank's seeded demo data, which the readiness
     * check below depends on.
     */
    private static final DockerImageName IMAGE = DockerImageName.parse(
            "parasoft/parabank@sha256:d686150499d6d655289c433802800dfc471166382660bd91bd9fd8d7f3ae71c3");

    private static final int HTTP_PORT = 8080;

    /**
     * Readiness is an HTTP 200 from the application's home page, not a bound port.
     *
     * <p>Measured on this image: the port accepts TCP connections 0.2s after start and the
     * first request is served 5.3s later, so a port-binding wait would hand the suite a
     * container that refuses every request for five seconds.
     *
     * <p>This endpoint looks like the weaker choice — the {@code @api} scenarios use the REST
     * layer, not the JSP UI — and waiting on a REST endpoint instead was tried first. It does
     * not work, for a reason worth recording: <strong>ParaBank initialises its HSQLDB schema
     * on the first request to the web application</strong>. Polling only
     * {@code /parabank/services/bank/accounts/12345} never succeeds; the container answers for
     * 90 seconds with {@code object not found: ACCOUNT} because nothing has triggered the
     * initialisation. Requesting this page once makes the REST layer answer 0.1s later.
     *
     * <p>So the wait is not merely observing readiness, it is causing it. That is worth knowing
     * before anyone "improves" this to probe the layer under test.
     */
    private static final String READINESS_PATH = "/parabank/index.htm";

    private static final Duration READINESS_TIMEOUT = Duration.ofMinutes(3);

    ParaBankContainer() {
        super(IMAGE);
        withExposedPorts(HTTP_PORT);
        waitingFor(Wait.forHttp(READINESS_PATH).forStatusCode(200).withStartupTimeout(READINESS_TIMEOUT));
        // Reuse is deliberately not enabled. Every clean run starts a fresh ParaBank: a reused
        // container carries the previous run's customers and balances, and a suite whose
        // scenarios own their data must not inherit anyone else's.
        withReuse(false);
    }

    /** The base URI other code should talk to, valid only while the container is running. */
    String baseUri() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT);
    }
}
