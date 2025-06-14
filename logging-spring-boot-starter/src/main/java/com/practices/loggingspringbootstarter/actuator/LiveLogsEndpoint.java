package com.practices.loggingspringbootstarter.actuator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.CyclicBufferAppender;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * A custom Spring Boot Actuator endpoint that exposes the most recent log entries captured by an
 * in-memory appender.
 *
 * <p>This endpoint is useful for debugging live application behavior without accessing external log
 * aggregators or files. It retrieves logs stored in a {@link CyclicBufferAppender} (usually
 * limited to 250 entries) and formats them for direct consumption by a UI or REST client.
 *
 * <p>Example usage: {@code /actuator/live-logs}
 */
@Endpoint(id = "live-logs")
public class LiveLogsEndpoint {

  private static final String IN_MEMORY_APPENDER_NAME = "IN_MEMORY_APPENDER";
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

  /**
   * Retrieves and formats the most recent log events from the in-memory appender.
   *
   * <p>The log output includes timestamp, log level, thread name, logger name, and message. This
   * can be used in embedded UIs or system dashboards for real-time visibility into application
   * behavior.
   *
   * @return a list of formatted log strings, or an error message if the appender is not found.
   */
  @ReadOperation
  public List<String> getLiveLogs() {
    final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    final Logger rootLogger = context.getLogger(ROOT_LOGGER_NAME);
    final CyclicBufferAppender<ILoggingEvent> appender =
        (CyclicBufferAppender<ILoggingEvent>) rootLogger.getAppender(IN_MEMORY_APPENDER_NAME);

    if (appender == null) {
      return Collections.singletonList(
          "Error: In-memory appender named '" + IN_MEMORY_APPENDER_NAME + "' not found.");
    }

    return IntStream.range(0, appender.getLength())
        .mapToObj(appender::get)
        .map(this::formatEvent)
        .toList();
  }

  private String formatEvent(final ILoggingEvent event) {
    return String.format(
        "%s %-5s [%s] --- %s: %s",
        FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())),
        event.getLevel(),
        event.getThreadName(),
        event.getLoggerName(),
        event.getFormattedMessage());
  }
}
