package dev.ozgurcetintas.trident.core.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

/**
 * Typed view over Trident's configuration.
 *
 * <p>The load policy is {@link LoadType#MERGE} rather than Owner's default {@code FIRST}.
 * {@code FIRST} stops at the first source that exists, which would make
 * {@code default.properties} unreachable whenever an environment-specific file is present.
 * {@code MERGE} makes the two sources behave as a fallback chain: a key defined in
 * {@code config/${env}.properties} wins, and every key it omits falls back to
 * {@code config/default.properties}.
 */
@LoadPolicy(LoadType.MERGE)
@Sources({"classpath:config/${env}.properties", "classpath:config/default.properties"})
public interface TridentConfig extends Config {

    String env();

    @Key("web.base.url")
    String webBaseUrl();

    @Key("api.base.url")
    String apiBaseUrl();

    @Key("timeout.default.seconds")
    long defaultTimeoutSeconds();

    @Key("timeout.polling.millis")
    long pollingMillis();
}
