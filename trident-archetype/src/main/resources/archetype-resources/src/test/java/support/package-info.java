/**
 * Support code that is not glue: page objects, clients, builders, anything your steps lean on.
 *
 * <p>It is a separate package from {@code steps} on purpose. Cucumber scans glue packages
 * recursively and runs {@code @BeforeAll} for everything it finds, so a container started from
 * a class in a glue package starts for every suite that lists it — including the fast one that
 * is supposed to need nothing. Keep lifecycle code here, and list this package on
 * {@code cucumber.glue} only for the suite that should pay for it.
 */
package ${package}.support;
