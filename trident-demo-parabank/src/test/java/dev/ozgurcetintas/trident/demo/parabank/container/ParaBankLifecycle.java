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
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    /**
     * Keys the container's address is published under. One address, two keys: the REST layer
     * and the browser talk to the same application, and TridentConfig keeps them separate so a
     * consumer whose UI and API live on different hosts can say so.
     */
    private static final String API_BASE_URL_PROPERTY = "api.base.url";

    private static final String WEB_BASE_URL_PROPERTY = "web.base.url";

    /** The REST endpoint the @api scenarios use, asked once before any scenario starts. */
    private static final String REST_READINESS_PATH = "/parabank/services/bank/accounts/12345";

    private static final Duration REST_READINESS_TIMEOUT = Duration.ofMinutes(1);

    /**
     * How many ParaBank instances to run. One by default, which is what ships.
     *
     * <p>ADR 0018 measured that this application cannot create two rows at once, so scenario
     * level parallelism against a single instance fails about half its registrations. More than
     * one instance is the other way to isolate: give each thread its own application rather
     * than trying to make one application concurrent. Set
     * {@code -Dtrident.parabank.instances=4} to try it.
     */
    private static final String INSTANCES_PROPERTY = "trident.parabank.instances";

    private static final List<ParaBankContainer> CONTAINERS = new CopyOnWriteArrayList<>();

    /** Instance addresses not yet claimed by a thread. */
    private static final Queue<String> UNCLAIMED = new ConcurrentLinkedQueue<>();

    /** The instance this thread claimed, held for the life of the thread. */
    private static final ThreadLocal<String> CLAIMED = new ThreadLocal<>();

    private static ParaBankContainer container;

    @BeforeAll
    public static void startParaBank() {
        int instances = Math.max(1, Integer.getInteger(INSTANCES_PROPERTY, 1));
        Instant start = Instant.now();

        // Started together rather than one after another: four sequential starts would cost
        // four times the wait, and the whole question is whether the wall clock improves.
        try (ExecutorService pool = Executors.newFixedThreadPool(instances)) {
            List<Future<ParaBankContainer>> pending = new ArrayList<>();
            for (int i = 0; i < instances; i++) {
                pending.add(pool.submit(() -> {
                    ParaBankContainer started = new ParaBankContainer();
                    started.start();
                    return started;
                }));
            }
            for (Future<ParaBankContainer> future : pending) {
                CONTAINERS.add(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting ParaBank", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("A ParaBank instance failed to start", e.getCause());
        }

        container = CONTAINERS.get(0);
        CONTAINERS.forEach(each -> UNCLAIMED.add(each.baseUri()));
        Duration startup = Duration.between(start, Instant.now());

        // Published as a system property, which is the highest layer of the existing
        // configuration precedence, and set here because @BeforeAll runs before any step can
        // touch ConfigProvider.get() — which resolves once and caches for the life of the JVM.
        // The alternative, a setter on ConfigProvider, would put knowledge of a container into
        // a published framework module.
        System.setProperty(API_BASE_URL_PROPERTY, container.baseUri());
        System.setProperty(WEB_BASE_URL_PROPERTY, container.baseUri());

        CONTAINERS.forEach(each -> awaitRestApi(each.baseUri()));

        LOG.info(
                "ParaBank started: {} instance(s) in {} ms at {}",
                CONTAINERS.size(),
                startup.toMillis(),
                CONTAINERS.stream().map(ParaBankContainer::baseUri).toList());
    }

    @AfterAll
    public static void stopParaBank() {
        CONTAINERS.forEach(ParaBankContainer::stop);
        if (!CONTAINERS.isEmpty()) {
            LOG.info("ParaBank stopped: {} instance(s)", CONTAINERS.size());
        }
        CONTAINERS.clear();
        UNCLAIMED.clear();
        container = null;
    }

    /**
     * The ParaBank this thread talks to.
     *
     * <p>A thread claims an instance the first time it asks and keeps it, so every request from
     * one scenario reaches one application and no two scenarios share one. With a single
     * instance — the shipped configuration — every thread claims the same address and this is
     * the value configuration already held.
     *
     * <p>Deliberately not a setter on {@code ConfigProvider}. Configuration is an input to a
     * run, not something a test may change underneath other tests, and a framework module has
     * no business knowing that a target comes in instances. The address is applied where it is
     * known: on the request specification, which accepts an override without the framework
     * being told anything.
     *
     * @return this thread's base URI, or the configured one when no instance is running
     */
    public static String baseUriForThisThread() {
        String mine = CLAIMED.get();
        if (mine != null) {
            return mine;
        }
        if (CONTAINERS.isEmpty()) {
            return ConfigProvider.get().apiBaseUrl();
        }
        String claimed = UNCLAIMED.poll();
        if (claimed == null) {
            // More threads than instances. Sharing is still correct, only slower and back to
            // being exposed to the defect in ADR 0018, so it is said out loud rather than
            // discovered later in a message about a username that already exists.
            claimed = CONTAINERS.get(0).baseUri();
            LOG.warn(
                    "More threads than ParaBank instances: thread {} is sharing {}. "
                            + "Set -D{}={} to give every thread its own.",
                    Thread.currentThread().getName(),
                    claimed,
                    INSTANCES_PROPERTY,
                    CONTAINERS.size() + 1);
        }
        CLAIMED.set(claimed);
        return claimed;
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
    @Before("@api or @web")
    public void requireParaBank() {
        if (container == null || !container.isRunning()) {
            throw new IllegalStateException("ParaBank is not running, so no @api or @web scenario can pass. "
                    + "The suite starts it in @BeforeAll; check the container logs above for why "
                    + "it failed to start, and that Docker is available (docker info).");
        }
    }
}
