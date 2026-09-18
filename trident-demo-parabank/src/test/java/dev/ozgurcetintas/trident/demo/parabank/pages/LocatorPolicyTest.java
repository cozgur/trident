package dev.ozgurcetintas.trident.demo.parabank.pages;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Enforces the locator policy from
 * {@code docs/adr/0017-locator-strategy-for-an-unmodifiable-ui.md} by reading the page-object
 * sources.
 *
 * <p>A rule that lives only in a document is a rule that erodes: the first absolute XPath
 * arrives on a Friday under deadline, passes review because it works, and the second one has a
 * precedent. This test is the version that argues back.
 *
 * <p>Two things are banned, both of which make a locator depend on where an element sits rather
 * than on what it is:
 *
 * <ul>
 *   <li>An absolute XPath — a literal beginning {@code //} — which is anchored at the document
 *       root and breaks when any ancestor changes.
 *   <li>A positional index — {@code [2]}, {@code [last()]} — which pins the third cell rather
 *       than the balance and silently starts reading a different column.
 * </ul>
 *
 * <p>It scans string literals, so a locator assembled at run time from pieces would slip past.
 * That is a known limit and an acceptable one: the point is to stop the easy mistake, and
 * anyone who concatenates their way around this has stopped making it by accident.
 */
class LocatorPolicyTest {

    private static final Path PAGES = Path.of("src/test/java/dev/ozgurcetintas/trident/demo/parabank/pages");

    /** Every double-quoted literal, with escaped quotes allowed inside. */
    private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    private static final Pattern ABSOLUTE_XPATH = Pattern.compile("^\\s*//");

    private static final Pattern POSITIONAL_INDEX = Pattern.compile("\\[\\s*(\\d+|last\\s*\\(\\s*\\))\\s*]");

    @Test
    void noPageObjectUsesAnAbsoluteXPathOrAPositionalIndex() {
        List<String> violations = scan(PAGES);

        assertThat(violations)
                .as("Locator policy, see docs/adr/0017-locator-strategy-for-an-unmodifiable-ui.md. "
                        + "An absolute XPath depends on every ancestor; a positional index pins a "
                        + "position rather than a thing.")
                .isEmpty();
    }

    /**
     * The test above can only be trusted if it fails when it should. This feeds the scanner the
     * two banned shapes and asserts that it names them both.
     */
    @Test
    void theScannerRejectsTheShapesItIsMeantToReject() {
        Path bad = writeTemporarySource(
                """
                package example;

                final class BadLocators {
                    static final String ABSOLUTE = "//div[@id='rightPanel']//input";
                    static final String INDEXED = ".//table//tr/td[2]";
                    static final String LAST = ".//table//tr[last()]";
                    static final String FINE = ".//table[@id='accountTable']//a[normalize-space()='1']";
                    static final String ALSO_FINE = "input[type='submit'][value='Log In']";
                }
                """);

        List<String> violations = scan(bad.getParent());

        assertThat(violations).hasSize(3);
        assertThat(String.join("\n", violations))
                .contains("absolute XPath")
                .contains("positional index")
                .contains("//div[@id='rightPanel']//input")
                .contains(".//table//tr/td[2]")
                .contains(".//table//tr[last()]");
    }

    /**
     * A guard on the guard. If the sources ever move, the scan above would find no files, find
     * no violations and pass while checking nothing.
     */
    @Test
    void theScannerIsLookingAtRealSources() {
        assertThat(sourcesIn(PAGES))
                .as("the page-object sources should be at %s", PAGES.toAbsolutePath())
                .isNotEmpty();
    }

    private static List<String> scan(Path directory) {
        List<String> violations = new ArrayList<>();
        for (Path source : sourcesIn(directory)) {
            String text = read(source);
            Matcher literals = STRING_LITERAL.matcher(text);
            while (literals.find()) {
                String literal = literals.group(1);
                if (ABSOLUTE_XPATH.matcher(literal).find()) {
                    violations.add(source.getFileName() + ": absolute XPath -> " + literal);
                }
                if (POSITIONAL_INDEX.matcher(literal).find()) {
                    violations.add(source.getFileName() + ": positional index -> " + literal);
                }
            }
        }
        return violations;
    }

    private static List<Path> sourcesIn(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    // This test is not a page object, and it quotes the banned shapes on purpose.
                    .filter(path -> !path.getFileName().toString().equals("LocatorPolicyTest.java"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path writeTemporarySource(String content) {
        try {
            Path directory = Files.createTempDirectory("trident-locator-policy");
            directory.toFile().deleteOnExit();
            Path source = directory.resolve("BadLocators.java");
            Files.writeString(source, content);
            source.toFile().deleteOnExit();
            return source;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
