package com.practices.loggingspringbootstarter.config;

import com.practices.loggingspringbootstarter.core.LoggingContext;
import com.practices.loggingspringbootstarter.web.MdcPopulatingFilterReactive;
import com.practices.loggingspringbootstarter.web.MdcPopulatingFilterServlet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.server.WebFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class WebLoggingAutoConfigurationTest {
  private final WebApplicationContextRunner webContextRunner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(WebLoggingAutoConfiguration.class))
          .withBean(LoggingContext.class, () -> mock(LoggingContext.class))
          .withPropertyValues("spring.main.web-application-type=*");

  private final ReactiveWebApplicationContextRunner reactiveWebContextRunner =
      new ReactiveWebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(WebLoggingAutoConfiguration.class))
          .withBean(LoggingContext.class, () -> mock(LoggingContext.class));

  @Test
  @DisplayName("should provide all core web beans when auto-configuration is active")
  void shouldProvideWebBeansInServletWebContext() {
    webContextRunner.run(context -> {
      assertThat(context).hasSingleBean(FilterRegistrationBean.class);
      FilterRegistrationBean<?> registrationBean =
          context.getBean(FilterRegistrationBean.class);
      assertThat(registrationBean.getFilter()).isInstanceOf(MdcPopulatingFilterServlet.class);
    });
  }

  @Test
  @DisplayName("should provide all core reactive web beans when auto-configuration is active")
  void shouldProvideWebBeansInReactiveWebContext() {
    reactiveWebContextRunner.run(context -> {
      assertThat(context).hasSingleBean(WebFilter.class);
      assertThat(context.getBean(WebFilter.class)).isInstanceOf(MdcPopulatingFilterReactive.class);
    });
  }

  @Test
  @DisplayName("should not provide the web-specific beans in a non-web scenario")
  void shouldNotProvideWebBeansInNonWebContext() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WebLoggingAutoConfiguration.class))
        .withBean(LoggingContext.class, () -> mock(LoggingContext.class))
        .withPropertyValues("spring.main.web-application-type=none")
        .run(context -> {
          assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
          assertThat(context).doesNotHaveBean(MdcPopulatingFilterServlet.class);
        });
  }
}
