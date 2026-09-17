package dev.ozgurcetintas.trident.demo.parabank.container;

import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
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
     * Fails once, clearly, if the container is not up.
     *
     * <p>Without this the first scenario reports a connection refused from somewhere inside
     * REST Assured, and every scenario after it repeats the same error, so the log is long and
     * the cause is not in it.
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
