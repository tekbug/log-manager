package com.practices.loggingcore.config;

import com.practices.loggingcore.core.LoggingContext;
import com.practices.loggingcore.web.MdcPopulatingFilterReactive;
import com.practices.loggingcore.web.MdcPopulatingFilterServlet;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.server.WebFilter;

/** A conditional autoconfiguration for web-specific features. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.ANY)
public class WebLoggingAutoConfiguration {
  /**
   * Registers an {@link MdcPopulatingFilterServlet} as a servlet filter in the web application context.
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
   *         the {@link MdcPopulatingFilterServlet} with Spring Boot
   */
  @Bean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  public FilterRegistrationBean<MdcPopulatingFilterServlet> webLoggingFilter(
      final LoggingContext loggingContext) {
    final FilterRegistrationBean<MdcPopulatingFilterServlet> filterRegistrationBean =
        new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new MdcPopulatingFilterServlet(loggingContext));
    filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return filterRegistrationBean;
  }

  @Bean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
  public WebFilter reactiveWebLoggingFilter(LoggingContext loggingContext) {
    return new MdcPopulatingFilterReactive(loggingContext);
  }
}
