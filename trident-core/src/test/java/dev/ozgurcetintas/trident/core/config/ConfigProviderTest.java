package dev.ozgurcetintas.trident.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises stage 1 of the bootstrap. {@link ConfigProvider#resolveEnvironment} is a pure
 * function, so these tests never trigger the cached instance behind
 * {@link ConfigProvider#get()}.
 */
class ConfigProviderTest {

    @Test
    @DisplayName("A system property wins over the ENV variable")
    void systemPropertyWins() {
        assertThat(ConfigProvider.resolveEnvironment(Map.of("env", "ci"), Map.of("ENV", "staging")))
                .isEqualTo("ci");
    }

    @Test
    @DisplayName("The ENV variable is used when no system property is set")
    void environmentVariableIsSecond() {
        assertThat(ConfigProvider.resolveEnvironment(Map.of(), Map.of("ENV", "staging")))
                .isEqualTo("staging");
    }

    @Test
    @DisplayName("Falls back to local when neither is set")
    void fallsBackToLocal() {
        assertThat(ConfigProvider.resolveEnvironment(Map.of(), Map.of())).isEqualTo("local");
    }
}
