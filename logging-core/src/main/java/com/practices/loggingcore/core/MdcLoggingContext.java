package com.practices.loggingcore.core;

import org.slf4j.MDC;

/**
 * The default implementation of {@link LoggingContext} that uses SLF4J's {@link MDC} as the
 * underlying storage mechanism.
 */
public final class MdcLoggingContext implements LoggingContext {
  @Override
  public void set(final String key, final String value) {
    MDC.put(key, value);
  }

  @Override
  public String get(final String key) {
    return MDC.get(key);
  }

  @Override
  public void remove(final String key) {
    MDC.remove(key);
  }

  @Override
  public AutoCloseable with(final String key, final String value) {
    MDC.put(key, value);
    return () -> MDC.remove(key);
  }

  @Override
  public void clearAll() {
    MDC.clear();
  }
}
