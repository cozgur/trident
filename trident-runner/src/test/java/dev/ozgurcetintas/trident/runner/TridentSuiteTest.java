package dev.ozgurcetintas.trident.runner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Pins the contract {@link TridentSuite} depends on.
 *
 * <p>The base class is only useful because the JUnit Platform's suite annotations are
 * {@code @Inherited}: a subclass that declares nothing but its feature selection is still a
 * Cucumber suite. That is a property of a third-party library, so it is asserted rather than
 * assumed — if a JUnit upgrade dropped it, every consumer's suite would silently stop being
 * discovered, and this test is what would catch it.
 *
 * <p>These tests need no target application, which is the point: trident-runner is publishable
 * and its own build must not require one.
 */
class TridentSuiteTest {

    @SelectClasspathResource("features")
    static class ConsumerSuite extends TridentSuite {}

    @Test
    @DisplayName("A subclass inherits @Suite without declaring it")
    void inheritsSuite() {
        assertThat(ConsumerSuite.class.isAnnotationPresent(Suite.class)).isTrue();
    }

    @Test
    @DisplayName("A subclass inherits the cucumber engine selection")
    void inheritsEngine() {
        IncludeEngines engines = ConsumerSuite.class.getAnnotation(IncludeEngines.class);

        assertThat(engines).isNotNull();
        assertThat(engines.value()).containsExactly("cucumber");
    }

    @Test
    @DisplayName("The base class is abstract, so it is never discovered as a suite itself")
    void baseIsAbstract() {
        assertThat(java.lang.reflect.Modifier.isAbstract(TridentSuite.class.getModifiers()))
                .isTrue();
    }
}
