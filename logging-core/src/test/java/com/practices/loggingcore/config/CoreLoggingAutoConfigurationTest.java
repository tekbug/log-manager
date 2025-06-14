package com.practices.loggingcore.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.practices.loggingcore.actuator.LiveLogsEndpoint;
import com.practices.loggingcore.aspect.LogContextAspect;
import com.practices.loggingcore.core.MdcLoggingContext;
import com.practices.loggingcore.core.LoggingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CoreLoggingAutoConfigurationTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(CoreLoggingAutoConfiguration.class));

  @Test
  @DisplayName("should provide all core beans when auto-configuration is active")
  void shouldProvideCoreBeans() {
    this.contextRunner.run(context -> {
          assertThat(context).hasSingleBean(LoggingContext.class);
          assertThat(context).hasSingleBean(LogContextAspect.class);
          assertThat(context).hasSingleBean(LiveLogsEndpoint.class);

          assertThat(context.getBean(LoggingContext.class))
              .isInstanceOf(MdcLoggingContext.class);
        });
  }
}
