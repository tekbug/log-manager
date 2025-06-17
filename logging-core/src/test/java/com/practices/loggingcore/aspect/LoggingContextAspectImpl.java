package com.practices.loggingcore.aspect;

import com.practices.loggingcore.core.LoggingContext;
import org.slf4j.MDC;

public class LoggingContextAspectImpl implements LoggingContext {

  @Override
  public void set(String key, String value) {
    if (key != null && value != null) {
      MDC.put(key, value);
    }
  }

  @Override
  public String get(String key) {
    return key != null ? MDC.get(key) : null;
  }

  @Override
  public void remove(String key) {
    if (key != null) {
      MDC.remove(key);
    }
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
