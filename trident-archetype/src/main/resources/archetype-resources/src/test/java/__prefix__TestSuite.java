package ${package};

import dev.ozgurcetintas.trident.runner.TridentSuite;
import org.junit.platform.suite.api.SelectClasspathResource;

/**
 * The fast suite. Surefire picks it up by the {@code *TestSuite} name and filters it to
 * {@code @smoke}, so it runs without a container and stays quick enough to run before pushing.
 *
 * <p>{@code @Suite} and the Cucumber engine selection are inherited from {@link TridentSuite}.
 * All this class decides is which features to select.
 */
@SelectClasspathResource("features")
public class ${prefix}TestSuite extends TridentSuite {}
