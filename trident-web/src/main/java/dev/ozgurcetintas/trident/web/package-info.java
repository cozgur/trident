/**
 * Web testing support: the browser lifecycle and the loadable page base.
 *
 * <p>Nothing in this package knows which application is under test. It supplies the mechanism —
 * building a driver from configuration, holding one per thread, waiting for a page to be ready —
 * and the consumer supplies the selectors, in page objects of their own.
 */
package dev.ozgurcetintas.trident.web;
