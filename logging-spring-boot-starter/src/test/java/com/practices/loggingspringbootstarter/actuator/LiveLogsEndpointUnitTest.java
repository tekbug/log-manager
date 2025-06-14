package com.practices.loggingspringbootstarter.actuator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("LiveLogsEndpoint Unit Tests")
class LiveLogsEndpointUnitTest {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

  private LiveLogsEndpoint endpoint;
  private Method formatEventMethod;

  @BeforeEach
  void setUp() throws Exception {
    endpoint = new LiveLogsEndpoint();

    formatEventMethod = LiveLogsEndpoint.class.getDeclaredMethod("formatEvent", ILoggingEvent.class);
    formatEventMethod.setAccessible(true);
  }

  @Test
  @DisplayName("Should format log event correctly with standard values")
  void shouldFormatLogEventCorrectlyWithStandardValues() throws Exception {

    long timestamp = 1701427845123L; // Fixed timestamp for consistent testing
    ILoggingEvent event = createLoggingEvent(timestamp, Level.INFO, "main",
        "com.practices.log.manager.test.Service", "Service started successfully");

    String result = (String) formatEventMethod.invoke(endpoint, event);

    String expectedTimestamp = FORMATTER.format(Instant.ofEpochMilli(timestamp));
    String expected = String.format("%s %-5s [%s] --- %s: %s",
        expectedTimestamp, "INFO", "main", "com.practices.log.manager.test.Service", "Service started successfully");

    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("logLevelTestData")
  @DisplayName("Should format different log levels with correct padding")
  void shouldFormatDifferentLogLevelsWithCorrectPadding(Level level, String expectedLevelStr) throws Exception {

    long timestamp = 1701427845123L;
    ILoggingEvent event = createLoggingEvent(timestamp, level, "main",
        "com.practices.log.manager.test.Test", "Test message");

    String result = (String) formatEventMethod.invoke(endpoint, event);

    assertThat(result).contains(expectedLevelStr);

    Pattern pattern = Pattern.compile("^(\\S+\\s+\\S+)\\s+([A-Z\\s]{5})\\s+.*");
    Matcher matcher = pattern.matcher(result);

    assertThat(matcher.find()).isTrue();
    String actualLevelStr = matcher.group(2);

    assertThat(actualLevelStr).isEqualTo(expectedLevelStr);
  }

  static Stream<Arguments> logLevelTestData() {
    return Stream.of(
        arguments(Level.TRACE, "TRACE"),
        arguments(Level.DEBUG, "DEBUG"),
        arguments(Level.INFO, "INFO "),
        arguments(Level.WARN, "WARN "),
        arguments(Level.ERROR, "ERROR")
    );
  }

  @Test
  @DisplayName("Should handle null formatted message")
  void shouldHandleNullFormattedMessage() throws Exception {
    
    LoggingEvent event = new LoggingEvent();
    event.setTimeStamp(1701427845123L);
    event.setLevel(Level.INFO);
    event.setThreadName("main");
    event.setLoggerName("com.practices.log.manager.test.Test");
    event.setMessage(null); // This will result in a null-formatted message

    // When
    String result = (String) formatEventMethod.invoke(endpoint, event);

    // Then
    assertThat(result).endsWith("com.practices.log.manager.test.Test: null");
  }

  @Test
  @DisplayName("Should handle empty formatted message")
  void shouldHandleEmptyFormattedMessage() throws Exception {
    // Given
    ILoggingEvent event = createLoggingEvent(1701427845123L, Level.INFO, "main",
        "com.practices.log.manager.test.Test", "");

    // When
    String result = (String) formatEventMethod.invoke(endpoint, event);

    // Then
    assertThat(result).endsWith("com.practices.log.manager.test.Test: ");
  }

  @Test
  @DisplayName("Should handle very long thread names")
  void shouldHandleVeryLongThreadNames() throws Exception {
    // Given
    String longThreadName = "very-long-thread-name-that-might-cause-formatting-issues-pool-1-thread-1234567890";
    ILoggingEvent event = createLoggingEvent(1701427845123L, Level.INFO, longThreadName,
        "com.practices.log.manager.test.Test", "Test message");

    String result = (String) formatEventMethod.invoke(endpoint, event);

    assertThat(result)
        .contains("[" + longThreadName + "]")
        .contains("Test message");
  }

  @Test
  @DisplayName("Should handle very long logger names")
  void shouldHandleVeryLongLoggerNames() throws Exception {
    String longLoggerName = "com.practices.log.manager.test.very.long.package.name.with.many.components.and.subpackages.Service";
    ILoggingEvent event = createLoggingEvent(1701427845123L, Level.INFO, "main",
        longLoggerName, "Test message");

    String result = (String) formatEventMethod.invoke(endpoint, event);

    assertThat(result).contains(longLoggerName + ": Test message");
  }

  @Test
  @DisplayName("Should handle messages with newlines and special characters")
  void shouldHandleMessagesWithNewlinesAndSpecialCharacters() throws Exception {

    String messageWithNewlines = "First line\nSecond line\nThird line";
    String messageWithTabs = "Column1\tColumn2\tColumn3";
    String messageWithSpecialChars = "Special chars: àáâãäåæçèéêë 你好 🌍";
    ILoggingEvent event1 = createLoggingEvent(1701427845123L, Level.INFO, "main",
        "com.practices.log.manager.test.Test1", messageWithNewlines);
    ILoggingEvent event2 = createLoggingEvent(1701427845123L, Level.INFO, "main",
        "com.practices.log.manager.test.Test2", messageWithTabs);
    ILoggingEvent event3 = createLoggingEvent(1701427845123L, Level.INFO, "main",
        "com.practices.log.manager.test.Test3", messageWithSpecialChars);

    String result1 = (String) formatEventMethod.invoke(endpoint, event1);
    String result2 = (String) formatEventMethod.invoke(endpoint, event2);
    String result3 = (String) formatEventMethod.invoke(endpoint, event3);

    assertThat(result1).contains(messageWithNewlines);
    assertThat(result2).contains(messageWithTabs);
    assertThat(result3).contains(messageWithSpecialChars);
  }

  @Test
  @DisplayName("Should format timestamp consistently across different times")
  void shouldFormatTimestampConsistentlyAcrossDifferentTimes() throws Exception {

    long[] timestamps = {
        0L, // Epoch
        1701427845123L, // Fixed test time
        System.currentTimeMillis(), // Current time
        Long.MAX_VALUE / 1000 // Far future (but valid)
    };
    for (long timestamp : timestamps) {
      ILoggingEvent event = createLoggingEvent(timestamp, Level.INFO, "main",
          "com.practices.log.manager.test.Test", "Test message for timestamp " + timestamp);

      String result = (String) formatEventMethod.invoke(endpoint, event);

      String expectedTimestamp = FORMATTER.format(Instant.ofEpochMilli(timestamp));
      assertThat(result).startsWith(expectedTimestamp);
      assertThat(result).matches("\\+?\\d+-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\w+\\s+\\[.*] --- .*: .*");
    }
  }

  @Test
  @DisplayName("Should maintain consistent format structure across all variations")
  void shouldMaintainConsistentFormatStructureAcrossAllVariations() throws Exception {

    Object[][] testData = {
        {Level.TRACE, "t1", "com.a.B", "msg1"},
        {Level.DEBUG, "thread-pool-1", "com.practices.log.manager.test.Service", "Debug message"},
        {Level.INFO, "main", "ROOT", "Info message"},
        {Level.WARN, "background-worker", "com.practices.log.manager.test.very.long.package.Name", "Warning message"},
        {Level.ERROR, "scheduler-1", "c.e.S", "Error occurred"}
    };

    for (Object[] data : testData) {
      Level level = (Level) data[0];
      String threadName = (String) data[1];
      String loggerName = (String) data[2];
      String message = (String) data[3];

      ILoggingEvent event = createLoggingEvent(1701427845123L, level, threadName, loggerName, message);

      String result = (String) formatEventMethod.invoke(endpoint, event);

      String[] parts = result.split(" --- ");
      assertThat(parts).hasSize(2);

      assertThat(parts[0]).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\w+\\s* \\[.*]");

      assertThat(parts[1]).startsWith(loggerName + ": " + message);
    }
  }

  /**
   * Helper method to create a LoggingEvent with the specified parameters
   */
  private ILoggingEvent createLoggingEvent(long timestamp, Level level, String threadName,
                                           String loggerName, String message) {
    LoggingEvent event = new LoggingEvent();
    event.setTimeStamp(timestamp);
    event.setLevel(level);
    event.setThreadName(threadName);
    event.setLoggerName(loggerName);
    event.setMessage(message);
    return event;
  }
}