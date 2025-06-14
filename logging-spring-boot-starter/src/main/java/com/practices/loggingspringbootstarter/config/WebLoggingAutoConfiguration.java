package com.practices.loggingspringbootstarter.config;

import com.practices.loggingspringbootstarter.core.LoggingContext;
import com.practices.loggingspringbootstarter.web.MdcPopulatingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/** A conditional autoconfiguration for web-specific features. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebLoggingAutoConfiguration {
  /**
   * Registers an {@link MdcPopulatingFilter} as a servlet filter in the web application context.
   *
   * <p>This filter populates the Mapped Diagnostic Context (MDC) with relevant
   * contextual information extracted from each incoming HTTP request, enabling enriched and
   * consistent logging throughout the lifecycle of the request.
   * </p>
   *
   * <p>The filter is registered with a high precedence order to ensure that MDC is populated early
   * in the filter chain, so all later processing and logging within the request scope
   * benefits from the contextual data.
   * </p>
   *
   * @param loggingContext the {@link LoggingContext} used by the filter to manage MDC entries
   * @return a {@link FilterRegistrationBean} that registers
   *         the {@link MdcPopulatingFilter} with Spring Boot
   */
  @Bean
  public FilterRegistrationBean<MdcPopulatingFilter> webLoggingFilter(
      final LoggingContext loggingContext) {
    final FilterRegistrationBean<MdcPopulatingFilter> filterRegistrationBean =
        new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new MdcPopulatingFilter(loggingContext));
    filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return filterRegistrationBean;
  }
}
