package com.practices.loggingspringbootstarter.core;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Unit tests for the {@link MdcLoggingContext} class.
 *
 * <p>This test suite validates that the context is correctly manipulated and, most importantly,
 * that state is properly isolated between tests by leveraging setup and teardown methods.
 */
@DisplayName("MdcLoggingContext - Core Logging Context Tests")
class MdcLoggingContextTest {

  private static final String TEST_KEY = "testKey";
  private static final String TEST_VALUE = "testValue";
  private static final String ANOTHER_KEY = "anotherKey";
  private static final String ANOTHER_VALUE = "anotherValue";

  private final LoggingContext loggingContext = new LoggingContextImpl();

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("put() should add a key-value pair to the MDC")
  void put_shouldAddValueToMdc() {

    loggingContext.set(TEST_KEY, TEST_VALUE);

    assertThat(MDC.get(TEST_KEY)).isEqualTo(TEST_VALUE);
  }

  @Test
  @DisplayName("set() should overwrite an existing value for the same key")
  void set_shouldOverwriteExistingValue() {

    loggingContext.set(TEST_KEY, "initialValue");

    loggingContext.set(TEST_KEY, "newValue");

    assertThat(MDC.get(TEST_KEY)).isEqualTo("newValue");
  }

  @Test
  @DisplayName("get() should retrieve a value from the MDC")
  void get_shouldRetrieveValueFromMdc() {
    MDC.put(TEST_KEY, TEST_VALUE);

    String result = loggingContext.get(TEST_KEY);

    assertThat(result).isEqualTo(TEST_VALUE);
  }

  @Test
  @DisplayName("get() should return null for a non-existent key")
  void get_shouldReturnNullForNonExistentKey() {

    String result = loggingContext.get("nonExistentKey");

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("remove() should remove a key-value pair from the MDC")
  void remove_shouldRemoveValueFromMdc() {

    MDC.put(TEST_KEY, TEST_VALUE);
    assertThat(MDC.get(TEST_KEY)).isNotNull(); // Pre-condition

    loggingContext.remove(TEST_KEY);

    assertThat(MDC.get(TEST_KEY)).isNull();
  }

  @Test
  @DisplayName("remove() should do nothing for a non-existent key")
  void remove_shouldDoNothingForNonExistentKey() {
    loggingContext.remove("nonExistentKey");
    assertThat(MDC.getCopyOfContextMap()).isNull();
  }



  @Test
  @DisplayName("remove() should do nothing for a non-existent key")
  void remove_shouldDoNothingForNonExistentKeyWhenParameterized() {
    loggingContext.remove("nonExistentKey");
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    assertThat(contextMap == null || contextMap.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("clearAll() should remove all entries from the MDC")
  void clearAll_shouldRemoveAllEntries() { // Renamed test for clarity
    // GIVEN
    MDC.put(TEST_KEY, TEST_VALUE);
    MDC.put(ANOTHER_KEY, ANOTHER_VALUE);
    assertThat(MDC.getCopyOfContextMap()).hasSize(2); // Pre-condition

    // WHEN
    loggingContext.clearAll(); // Call the correctly named method

    // THEN
    // This test will now pass.
    assertThat(MDC.getCopyOfContextMap()).isEmpty();
  }

  @Test
  @DisplayName("withContext() should add a value for the scope of a try-with-resources block")
  void withContext_shouldAddValueForScopeAndRemoveAfter() {
    assertThat(MDC.get(TEST_KEY)).isNull();

    try (AutoCloseable ignored = loggingContext.with(TEST_KEY, TEST_VALUE)) {
      assertThat(MDC.get(TEST_KEY)).isEqualTo(TEST_VALUE);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertThat(MDC.get(TEST_KEY)).isNull();
  }

  @Test
  @DisplayName("withContext() should correctly function even if an exception is thrown")
  void withContext_shouldRemoveValueEvenIfExceptionIsThrown() {
    // given
    assertThat(MDC.get(TEST_KEY)).isNull();
    Exception expectedException = new RuntimeException("Intentional test exception");

    try {
      try (AutoCloseable ignored = loggingContext.with(TEST_KEY, TEST_VALUE)) {
        assertThat(MDC.get(TEST_KEY)).isEqualTo(TEST_VALUE);
        throw expectedException;
      }
    } catch (Exception e) {
      assertThat(e).isSameAs(expectedException);
    }

    assertThat(MDC.get(TEST_KEY)).isNull();
  }
}
