package com.practices.loggingspringbootstarter.actuator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.CyclicBufferAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * Provides integration tests for the {@link LiveLogsEndpoint}.
 * <p>
 * These tests work by programmatically interacting with the Logback logging framework.
 * For each test, a {@link CyclicBufferAppender} is created and attached to the root logger.
 * This appender captures all log events generated during the test in an in-memory buffer.
 * <p>
 * A dedicated test logger is used to generate specific log events with varying levels,
 * messages, and contexts. The {@code LiveLogsEndpoint.getLiveLogs()} method is then invoked,
 * which is expected to find the in-memory appender, retrieve the buffered events, and format
 * them into a list of strings.
 * <p>
 * Assertions are used to verify various aspects of the endpoint's behavior, including:
 * <ul>
 *     <li>Correct formatting of log messages (timestamp, level, thread, logger, message).</li>
 *     <li>Handling of cases where the appender is missing or has no events.</li>
 *     <li>Properly capturing different log levels and special characters.</li>
 *     <li>Maintaining the chronological order of log events.</li>
 *     <li>Correctly handling buffer overflows and concurrent logging from multiple threads.</li>
 * </ul>
 * The in-memory appender is detached and stopped after each test to ensure test isolation.
 */
@DisplayName("LiveLogsEndpoint Integration Tests")
class LiveLogsEndpointTest {

  private static final String IN_MEMORY_APPENDER_NAME = "IN_MEMORY_APPENDER";
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

  private LiveLogsEndpoint endpoint;
  private LoggerContext loggerContext;
  private Logger rootLogger;
  private CyclicBufferAppender<ILoggingEvent> appender;
  private Logger testLogger;

  @BeforeEach
  void setUp() {
    endpoint = new LiveLogsEndpoint();
    loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);

    appender = new CyclicBufferAppender<>();
    appender.setName(IN_MEMORY_APPENDER_NAME);
    appender.setContext(loggerContext);
    appender.setMaxSize(250);
    appender.start();

    rootLogger.addAppender(appender);
    testLogger = loggerContext.getLogger("com.example.TestLogger");
  }

  @AfterEach
  void tearDown() {
    if (rootLogger != null && appender != null) {
      rootLogger.detachAppender(appender);
    }
    if (appender != null) {
      appender.stop();
    }
  }

  @Test
  @DisplayName("Should return formatted logs when appender exists and has events")
  void shouldReturnFormattedLogsWhenAppenderExistsAndHasEvents() {
    testLogger.info("Service started successfully");
    testLogger.warn("Connection pool nearly exhausted");
    testLogger.error("Database connection failed");

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    String resultString = String.join("\n", result);
    assertThat(resultString).contains("Service started successfully");
    assertThat(resultString).contains("Connection pool nearly exhausted");
    assertThat(resultString).contains("Database connection failed");

    for (String logEntry : result) {
      assertThat(logEntry).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\w+\\s+\\[.*] --- .*: .*");
    }
  }

  @Test
  @DisplayName("Should return error message when appender does not exist")
  void shouldReturnErrorMessageWhenAppenderDoesNotExist() {
    rootLogger.detachAppender(appender);

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(
        "Error: In-memory appender named 'IN_MEMORY_APPENDER' not found.");
  }

  @Test
  @DisplayName("Should return empty list when appender exists but has no events")
  void shouldReturnEmptyListWhenAppenderExistsButHasNoEvents() {
    List<String> result = endpoint.getLiveLogs();
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should handle different log levels correctly")
  void shouldHandleDifferentLogLevelsCorrectly() {
    testLogger.debug("Debug message");
    testLogger.info("Info message");
    testLogger.warn("Warn message");
    testLogger.error("Error message");

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    String resultString = String.join("\n", result);
    assertThat(resultString).contains("INFO ");
    assertThat(resultString).contains("WARN ");
    assertThat(resultString).contains("ERROR");

    for (String logEntry : result) {
      if (logEntry.contains("Info message")) {
        assertThat(logEntry).contains("INFO ");
      } else if (logEntry.contains("Warn message")) {
        assertThat(logEntry).contains("WARN ");
      } else if (logEntry.contains("Error message")) {
        assertThat(logEntry).contains("ERROR");
      }
    }
  }

  @Test
  @DisplayName("Should handle long logger names and messages correctly")
  void shouldHandleLongLoggerNamesAndMessagesCorrectly() {
    String longLoggerName = "com.practices.log.manager.very.long.package.name.with.many.components.Service";
    Logger longNameLogger = loggerContext.getLogger(longLoggerName);
    String longMessage = "This is a very long log message that contains a lot of information " +
        "and should be handled correctly by the formatting logic without truncation or issues";
    longNameLogger.info(longMessage);

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeGreaterThanOrEqualTo(1);
    String resultString = String.join("\n", result);
    assertThat(resultString).contains(longLoggerName);
    assertThat(resultString).contains(longMessage);
  }

  @Test
  @DisplayName("Should handle messages with special characters correctly")
  void shouldHandleMessagesWithSpecialCharactersCorrectly() {
    testLogger.info("Message with special chars: àáâãäåæçèéêë");
    testLogger.warn("Message with symbols: !@#$%^&*()_+-=[]|:;'<>,.?/");
    testLogger.error("Message with unicode: 你好世界 🌍 مرحبا بالعالم");

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    String resultString = String.join("\n", result);
    assertThat(resultString).contains("àáâãäåæçèéêë");
    assertThat(resultString).contains("!@#$%^&*()_+-=[]|:;'<>,.?/");
    assertThat(resultString).contains("你好世界 🌍 مرحبا بالعالم");
  }

  @Test
  @DisplayName("Should maintain chronological order of log events")
  void shouldMaintainChronologicalOrderOfLogEvents() {
    testLogger.info("First message");
    waitForNextMillisecond();

    testLogger.info("Second message");
    waitForNextMillisecond();

    testLogger.info("Third message");

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    int firstIndex = -1, secondIndex = -1, thirdIndex = -1;
    for (int i = 0; i < result.size(); i++) {
      if (result.get(i).contains("First message")) {
        firstIndex = i;
      } else if (result.get(i).contains("Second message")) {
        secondIndex = i;
      } else if (result.get(i).contains("Third message")) {
        thirdIndex = i;
      }
    }

    assertThat(firstIndex).isNotEqualTo(-1);
    assertThat(secondIndex).isNotEqualTo(-1);
    assertThat(thirdIndex).isNotEqualTo(-1);
    assertThat(firstIndex).isLessThan(secondIndex);
    assertThat(secondIndex).isLessThan(thirdIndex);
  }

  @Test
  @DisplayName("Should handle thread names correctly")
  void shouldHandleThreadNamesCorrectly() {
    String currentThreadName = Thread.currentThread().getName();
    testLogger.info("Message from main thread");

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeGreaterThanOrEqualTo(1);
    String resultString = String.join("\n", result);
    assertThat(resultString).contains("[" + currentThreadName + "]");
    assertThat(resultString).contains("Message from main thread");
  }

  @Test
  @DisplayName("Should handle cyclic buffer overflow correctly")
  void shouldHandleCyclicBufferOverflowCorrectly() {
    appender.setMaxSize(25);

    for (int i = 0; i < 10; i++) {
      testLogger.info("Log message {}", i);
    }

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeLessThanOrEqualTo(25);
    String resultString = String.join("\n", result);
    assertThat(resultString).contains("Log message 9");
  }

  @Test
  @DisplayName("Should format timestamp correctly")
  void shouldFormatTimestampCorrectly() {
    long beforeLogging = System.currentTimeMillis();
    testLogger.info("Timestamp test message");
    long afterLogging = System.currentTimeMillis();

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeGreaterThanOrEqualTo(1);
    String testLogEntry = result.stream()
        .filter(log -> log.contains("Timestamp test message"))
        .findFirst()
        .orElse("");
    assertThat(testLogEntry).isNotEmpty();

    String timestampStr = testLogEntry.substring(0, 23);
    assertThat(timestampStr).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");

    Instant loggedTimestamp = Instant.from(FORMATTER.parse(timestampStr));
    assertThat(loggedTimestamp.toEpochMilli()).isBetween(beforeLogging, afterLogging);
  }

  @Test
  @DisplayName("Should handle concurrent logging correctly")
  void shouldHandleConcurrentLoggingCorrectly() {
    Thread[] threads = new Thread[5];
    for (int i = 0; i < 5; i++) {
      final int threadId = i;
      threads[i] = new Thread(() -> {
        for (int j = 0; j < 3; j++) {
          testLogger.info("Thread-{} message-{}", threadId, j);
        }
      });
    }

    for (Thread thread : threads) {
      thread.start();
    }

    await().atMost(Duration.ofSeconds(2)).until(allThreadsAreTerminated(threads));

    List<String> result = endpoint.getLiveLogs();

    assertThat(result).hasSizeGreaterThanOrEqualTo(15);
    String resultString = String.join("\n", result);
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 3; j++) {
        assertThat(resultString).contains("Thread-" + i + " message-" + j);
      }
    }
  }

  /**
   * Pauses execution until the system clock advances to the next millisecond.
   * This is used to guarantee that consecutive log messages have distinct timestamps.
   */
  private void waitForNextMillisecond() {
    long startTime = System.currentTimeMillis();
    await().atMost(Duration.ofSeconds(1)).until(() -> System.currentTimeMillis() > startTime);
  }

  /**
   * Returns a {@link Callable} that checks if all provided threads have terminated.
   *
   * @param threads The array of threads to check.
   * @return A Callable that returns true if all threads are in the TERMINATED state, false otherwise.
   */
  private Callable<Boolean> allThreadsAreTerminated(Thread[] threads) {
    return () -> Arrays.stream(threads).allMatch(t -> t.getState() == Thread.State.TERMINATED);
  }
}