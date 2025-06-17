package com.practices.loggingcore.aspect;

import com.practices.loggingcore.annotation.LogContext;
import com.practices.loggingcore.core.LoggingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({LogContextAspectTest.TestConfig.class})
@EnableAspectJAutoProxy
@DisplayName("LogContext Aspect Integration Test")
class LogContextAspectTest {

  @Configuration
  @EnableAspectJAutoProxy
  static class TestConfig {

    @Bean
    public LoggingContext loggingContext() {
      return new LoggingContextAspectImpl();
    }

    @Bean
    public LogContextAspect logContextAspect(LoggingContext loggingContext) {
      return new LogContextAspect(loggingContext);
    }

    @Bean
    public TestService testService() {
      return new TestService();
    }

    @Bean
    public ClassAnnotatedService classService() {
      return new ClassAnnotatedService();
    }

    @Bean
    public OverriddenService overriddenService() {
      return new OverriddenService();
    }

    @Bean
    public FaultyService faultyService() {
      return new FaultyService();
    }

    @Bean
    public TestServiceNoParams testServiceWithNoParams() {
      return new TestServiceNoParams();
    }

    @Bean
    public TestServiceEmptyExpressions testServiceEmptyExpressions() {
      return new TestServiceEmptyExpressions();
    }
  }

  static class TestService {
    @LogContext(expressions = {"orderId=#id", "customerName=#customer.name"})
    public void processOrder(String id, Customer customer) {
      assertThat(MDC.get("orderId")).isEqualTo(id);
      assertThat(MDC.get("customerName")).isEqualTo(customer.name());
    }

    @LogContext(expressions = {"action=\"test\""})
    public void methodThatThrows() {
      assertThat(MDC.get("action")).isEqualTo("test");
      throw new IllegalStateException("Test exception");
    }
  }

  @LogContext(expressions = {"user=#user"})
  static class ClassAnnotatedService {
    public void greet(String user) {
      assertThat(MDC.get("user")).isEqualTo(user);
    }
  }

  @LogContext(expressions = {"globalKey=\"from-class\""})
  static class OverriddenService {
    @LogContext(expressions = {"methodKey=\"from-method\""})
    public void override() {
      assertThat(MDC.get("globalKey")).isNull();
      assertThat(MDC.get("methodKey")).isEqualTo("from-method");
    }
  }

  static class FaultyService {
    @LogContext(expressions = {
        "key1=#nonExistent.property",  // this should fail
        "key2=\"value\"",              // this should be valid
        "key2=\"duplicate\"",          // this should be ignored cause of duplicity
        "key3=null"                    // this should be skipped cause of null
    })
    public void faulty() {
      assertThat(MDC.get("key2")).isEqualTo("value"); // first occurrence wins
      assertThat(MDC.get("key1")).isNull();           // invalid expression, ignored
      assertThat(MDC.get("key3")).isNull();           // null value, skipped
    }
  }

  record Customer(String name) {}

  @Autowired
  private TestService testService;

  @Autowired
  private ClassAnnotatedService classAnnotatedService;

  @Autowired
  private OverriddenService overriddenService;

  @Autowired
  private FaultyService faultyService;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private TestServiceNoParams testServiceNoParams;

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("should populate MDC from method arguments and clear it afterwards")
  void shouldPopulateMdcFromArgumentsAndClear() {
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    testService.processOrder("order-123", new Customer("John Doe"));
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  @DisplayName("should clear MDC even when the annotated method throws an exception")
  void shouldClearMdcOnException() {
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    assertThatThrownBy(() -> testService.methodThatThrows())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Test exception");
    assertThat(MDC.get("action")).isNull();
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  @DisplayName("should apply @LogContext at class level")
  void testClassLevelAnnotation() {
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    classAnnotatedService.greet("Ayele");
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  @DisplayName("method-level annotation should override class-level annotation")
  void testMethodOverridesClassLevel() {
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    overriddenService.override();
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  @DisplayName("should handle invalid SpEL, nulls, and duplicates gracefully")
  void testInvalidAndDuplicateExpressions() {
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    faultyService.faulty();
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  @DisplayName("debug aspect application - verify beans are properly configured")
  void debugAspectApplication() {
    assertThat(applicationContext.containsBean("loggingContext")).isTrue();
    assertThat(applicationContext.containsBean("logContextAspect")).isTrue();
    assertThat(applicationContext.containsBean("testService")).isTrue();

    LogContextAspect aspect = applicationContext.getBean(LogContextAspect.class);
    assertThat(aspect).isNotNull();

    LoggingContext context = applicationContext.getBean(LoggingContext.class);
    assertThat(context).isNotNull();

    System.out.println("All beans configured properly:");
    System.out.println("- LogContextAspect: " + aspect);
    System.out.println("- LoggingContext: " + context);
  }

  @Test
  @DisplayName("should handle method with no parameters")
  void testMethodWithNoParameters() {
    testServiceNoParams.noParamsMethod();
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  @DisplayName("should handle empty expressions array")
  void testEmptyExpressions() {
    TestServiceEmptyExpressions service = new TestServiceEmptyExpressions();
    service.emptyExpressionsMethod();
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  static class TestServiceNoParams {
    @LogContext(expressions = {"staticKey=\"static-value\""})
    public void noParamsMethod() {
      assertThat(MDC.get("staticKey")).isEqualTo("static-value");
    }
  }

  static class TestServiceEmptyExpressions {
    @LogContext(expressions = {})
    public void emptyExpressionsMethod() {
      // should work fine, no MDC entries is expected
    }
  }
}