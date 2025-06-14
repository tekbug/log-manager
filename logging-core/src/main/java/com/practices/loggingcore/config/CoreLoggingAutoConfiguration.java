package com.practices.loggingcore.config;

import com.practices.loggingcore.actuator.LiveLogsEndpoint;
import com.practices.loggingcore.annotation.LogContext;
import com.practices.loggingcore.aspect.LogContextAspect;
import com.practices.loggingcore.core.LoggingContext;
import com.practices.loggingcore.core.MdcLoggingContext;
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
   * Provides the central bean for interacting with the logging context (MDC).
   *
   * @return An implementation of {@link LoggingContext}.
   */
  @Bean
  public LoggingContext loggingContext() {
    return new MdcLoggingContext();
  }

  /**
   * Provides the AOP aspect that powers the {@link LogContext}
   * annotation. This bean is responsible for intercepting annotated methods and enriching the MDC.
   *
   * @param loggingContext The central logging context service.
   * @return The aspect bean.
   */
  @Bean
  public LogContextAspect logContextAspect(final LoggingContext loggingContext) {
    return new LogContextAspect(loggingContext);
  }

  /**
   * Provides the custom Spring Boot Actuator endpoint for viewing live, in-memory logs.
   *
   * @return The {@link LiveLogsEndpoint} bean.
   */
  @Bean
  public LiveLogsEndpoint liveLogsEndpoint() {
    return new LiveLogsEndpoint();
  }
}
