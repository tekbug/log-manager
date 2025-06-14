package com.practices.loggingspringbootstarter.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.practices.loggingspringbootstarter.annotation.LogContext;
import com.practices.loggingspringbootstarter.config.CoreLoggingAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(CoreLoggingAutoConfiguration.class)
@DisplayName("LogContext Aspect Integration Test")
class LogContextAspectTest {

  @Configuration
  static class TestConfig {
    @Bean
    public TestService testService() {
      return new TestService();
    }
  }

  // A simple service with methods annotated with @LogContext for us to test.
  static class TestService {
    @LogContext(expressions = {"orderId=#id", "customerName=#customer.name"})
    public void processOrder(String id, Customer customer) {
      assertThat(MDC.get("orderId")).isEqualTo(id);
      assertThat(MDC.get("customerName")).isEqualTo(customer.name());
    }

    @LogContext(expressions = {"action=test"})
    public void methodThatThrows() {
      throw new IllegalStateException("Test exception");
    }
  }

  // A simple record for nested property testing.
  record Customer(String name) {}

  @Autowired private TestService testService;

  @AfterEach
  void tearDown() {
    // Ensure MDC is always clean after each test.
    MDC.clear();
  }

  @Test
  @DisplayName("should populate MDC from method arguments and clear it afterwards")
  void shouldPopulateMdcFromArgumentsAndClear() {
    // when
    testService.processOrder("order-123", new Customer("John Doe"));

    // then
    // The aspect's "finally" block should have cleared the MDC.
    assertThat(MDC.getCopyOfContextMap()).isEmpty();
  }

  @Test
  @DisplayName("should clear MDC even when the annotated method throws an exception")
  void shouldClearMdcOnException() {
    // when / then
    assertThatThrownBy(() -> testService.methodThatThrows())
        .isInstanceOf(IllegalStateException.class);

    // Assert that the aspect's "finally" block still ran and cleared the context.
    assertThat(MDC.get("action")).isNull();
  }
}
