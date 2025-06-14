package com.practices.loggingspringbootstarter.config;

import com.practices.loggingspringbootstarter.core.LoggingContext;
import com.practices.loggingspringbootstarter.core.MdcLoggingContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * This is the autoconfiguration class for all logging aspects other than web configs.
 */
@AutoConfiguration
@EnableAspectJAutoProxy
public class CoreLoggingAutoConfiguration {
  /**
   * A bean configuration for declaring a logging context from MDC.
   */
  @Bean
  public LoggingContext loggingContext() {
    return new MdcLoggingContext();
  }
}
