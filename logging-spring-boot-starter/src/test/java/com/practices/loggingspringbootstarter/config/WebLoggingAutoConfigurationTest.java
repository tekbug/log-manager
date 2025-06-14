package com.practices.loggingspringbootstarter.config;

import com.practices.loggingspringbootstarter.web.MdcPopulatingFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class WebLoggingAutoConfigurationTest {
  private final ApplicationContextRunner webContextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(WebLoggingAutoConfiguration.class));

  @Test
  @DisplayName("should provide all core web beans when auto-configuration is active")
  void shouldProvideWebBeansInWebContext() {
    this.webContextRunner.run(context -> {
      assertThat(context).hasSingleBean(FilterRegistrationBean.class);
      assertThat(context.getBean(FilterRegistrationBean.class).getFilter())
          .isInstanceOf(MdcPopulatingFilter.class);
    });
  }

  @Test
  @DisplayName("should NOT provide the web-specific beans in a non-web scenario")
  void shouldNotProvideWebBeansInWebContext() {
    this.webContextRunner.run(context -> {
      assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
      assertThat(context).doesNotHaveBean(MdcPopulatingFilter.class);
    });
  }
}
