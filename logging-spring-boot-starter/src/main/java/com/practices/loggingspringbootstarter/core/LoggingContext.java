package com.practices.loggingspringbootstarter.core;

/**
 * Defines the public contract for interacting with the logging context (MDC).
 *
 * <p>This is the primary interface for a more opinionated log handling mechanism with MDC.
 * It provides a higher-level, testable abstraction over the static {@link org.slf4j.MDC}.
 */
public interface LoggingContext {

  /**
   * Adds or updates a key-value pair in the logging context.
   *
   * @param key the context key (must not be null)
   * @param value the context value
   */
  void set(String key, String value);

  /**
   * Retrieves a value from the context for a given key.
   *
   * @param key the context key
   * @return the context value, or null if not present
   */
  String get(String key);

  /**
   * Removes a key from the logging context.
   *
   * @param key the context key to remove
   */
  void remove(String key);

  /**
   * Adds a key-value pair to the context for the duration of a {@code try-with-resources} block.
   * The key is automatically removed when the block is exited.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * try (var ignored = loggingContext.with("orderId", "123")) {
   *   log.info("Processing order."); // This log will contain orderId=123
   * }
   * // orderId is now removed from the context.
   * }</pre>
   *
   * @param key the context key (must not be null)
   * @param value the context value
   * @return an {@link AutoCloseable} that will remove the key upon being closed
   */
  AutoCloseable with(String key, String value);

  /** Clears the entire logging context for the current thread. */
  void clearAll();
}