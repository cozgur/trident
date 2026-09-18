package dev.ozgurcetintas.trident.api;

/**
 * A status code and a body, captured whatever the status was.
 *
 * @param statusCode the HTTP status
 * @param body the response body as text, never {@code null}
 */
public record ApiResponse(int statusCode, String body) {}
