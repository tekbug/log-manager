package com.practices.loggingcore.core;

import org.slf4j.MDC;

public class LoggingContextImpl implements LoggingContext {
  @Override
  public void set(String key, String value) {
    MDC.put(key, value);
  }

  @Override
  public String get(String key) {
    return MDC.get(key);
  }

  @Override
  public void remove(String key) {
    MDC.remove(key);
  }

  @Override
  public AutoCloseable with(String key, String value) {
    set(key, value);
    return () -> remove(key);
  }

  @Override
  public void clearAll() {
    MDC.clear();
  }
}
