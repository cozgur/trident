package dev.ozgurcetintas.trident.demo.parabank.fixtures;

/**
 * A customer this scenario created, and the account ParaBank opened with it.
 *
 * <p>Held in the scenario context, never in a static field: two scenarios must be able to run
 * in any order, and later in parallel, without either seeing the other's customer.
 *
 * @param username the login name, unique to one scenario
 * @param password the password registered with it
 * @param customerId ParaBank's id for the customer
 * @param initialAccountId the CHECKING account ParaBank opens on registration
 */
public record ParaBankCustomer(String username, String password, int customerId, int initialAccountId) {}
