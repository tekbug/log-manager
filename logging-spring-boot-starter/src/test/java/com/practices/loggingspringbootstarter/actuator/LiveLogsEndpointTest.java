package com.practices.loggingspringbootstarter.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.read.CyclicBufferAppender;
import com.practices.loggingspringbootstarter.config.CoreLoggingAutoConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(CoreLoggingAutoConfiguration.class)
@DisplayName("LiveLogs Actuator Endpoint Test")
class LiveLogsEndpointTest {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(LiveLogsEndpointTest.class);
  private static final String IN_MEMORY_APPENDER_NAME = "IN_MEMORY_APPENDER";

  @Autowired private TestRestTemplate restTemplate;
  private CyclicBufferAppender appender;
  private Logger rootLogger;

  @BeforeEach
  void setUp() {
    // Programmatically add our in-memory appender for the test.
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

    appender = new CyclicBufferAppender<>();
    appender.setName(IN_MEMORY_APPENDER_NAME);
    appender.setContext(context);
    appender.start();

    rootLogger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    // Detach the appender to ensure test isolation.
    if (rootLogger != null && appender != null) {
      rootLogger.detachAppender(appender);
    }
  }

  @Test
  @DisplayName("should return recent logs when called")
  void shouldReturnRecentLogs() {
    // given
    log.info("This is the first test message.");
    log.warn("This is the second test message.");

    // when
    ResponseEntity<List<String>> response =
        restTemplate.exchange(
            "/actuator/live-logs",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

    // then
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    List<String> logs = response.getBody();
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).contains("INFO").contains("first test message");
    assertThat(logs.get(1)).contains("WARN").contains("second test message");
  }

  @Test
  @DisplayName("should return an error message if appender is not configured")
  void shouldReturnErrorWhenAppenderIsMissing() {
    // given
    // Detach the appender we added in setUp to simulate a misconfiguration.
    rootLogger.detachAppender(appender);

    // when
    ResponseEntity<List<String>> response =
        restTemplate.exchange(
            "/actuator/live-logs",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

    // then
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    List<String> logs = response.getBody();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).contains("Error: In-memory appender named 'IN_MEMORY_APPENDER' not found.");
  }
}
