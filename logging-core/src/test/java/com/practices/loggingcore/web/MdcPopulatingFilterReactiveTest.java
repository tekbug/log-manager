package com.practices.loggingcore.web;

import com.practices.loggingcore.core.LoggingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MdcPopulatingFilterReactiveTest {

  private static final String HEADER_USER_ID = "X-User-ID";
  private static final String MDC_USER_ID = "userID";

  @Mock
  private LoggingContext loggingContext;

  @Mock
  private ServerWebExchange exchange;

  @Mock
  private ServerHttpRequest request;

  @Mock
  private WebFilterChain chain;

  private HttpHeaders headers;
  private MdcPopulatingFilterReactive filter;

  @BeforeEach
  void setUp() {
    headers = new HttpHeaders();
    filter = new MdcPopulatingFilterReactive(loggingContext);

    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);
    lenient().when(chain.filter(exchange)).thenReturn(Mono.empty());
  }

  @Test
  void shouldSetUserIdInLoggingContextWhenHeaderIsPresent() {
    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "\t", "\n", " \t \n "})
  void shouldNotSetUserIdWhenHeaderIsNullEmptyOrBlank(String headerValue) {

    headers.set(HEADER_USER_ID, headerValue);

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext, never()).set(eq(MDC_USER_ID), any());
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldSetUserIdWhenHeaderHasWhitespaceButIsNotBlank() {

    String userId = " user123 ";
    headers.set(HEADER_USER_ID, userId);

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldAlwaysClearContextEvenWhenChainReturnsError() {

    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);
    RuntimeException testException = new RuntimeException("Test exception");
    when(chain.filter(exchange)).thenReturn(Mono.error(testException));

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldAlwaysClearContextEvenWhenLoggingContextSetThrowsException() {

    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);
    RuntimeException testException = new RuntimeException("MDC set failed");
    doThrow(testException).when(loggingContext).set(MDC_USER_ID, userId);

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(chain, never()).filter(exchange); // Should not reach chain due to exception
    verify(loggingContext).clearAll();
    verify(chain, never()).filter(exchange);
  }

  @Test
  void shouldClearContextOnSuccessfulCompletion() {

    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldClearContextOnError() {

    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);
    RuntimeException testError = new RuntimeException("Test error");
    when(chain.filter(exchange)).thenReturn(Mono.error(testError));

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldClearContextOnCancellation() {

    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);
    when(chain.filter(exchange)).thenReturn(Mono.never()); // Never completes

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .thenCancel()
        .verify();

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldThrowNpeWhenContextIsNullAndSubscribed() {
    headers.set("X-User-ID", "user123");
    MdcPopulatingFilterReactive filterWithNullContext = new MdcPopulatingFilterReactive(null);

    assertThrows(NullPointerException.class, () -> {
      filterWithNullContext.filter(exchange, chain).block();
    });

    verify(chain, never()).filter(any(ServerWebExchange.class));
  }

  @Test
  void shouldProcessMultipleExchangesIndependently() {

    String firstUserId = "user123";
    String secondUserId = "user456";

    headers.set(HEADER_USER_ID, firstUserId);
    Mono<Void> firstResult = filter.filter(exchange, chain);

    StepVerifier.create(firstResult)
        .verifyComplete();

    reset(loggingContext, chain);
    when(chain.filter(exchange)).thenReturn(Mono.empty());
    headers.clear();
    headers.set(HEADER_USER_ID, secondUserId);

    Mono<Void> secondResult = filter.filter(exchange, chain);

    StepVerifier.create(secondResult)
        .verifyComplete();

    verify(loggingContext).set(MDC_USER_ID, secondUserId);
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldHandleSpecialCharactersInUserId() {

    String userIdWithSpecialChars = "user@domain.com|special-chars_123";
    headers.set(HEADER_USER_ID, userIdWithSpecialChars);

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext).set(MDC_USER_ID, userIdWithSpecialChars);
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldHandleVeryLongUserId() {

    String longUserId = "a".repeat(1000);
    headers.set(HEADER_USER_ID, longUserId);

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext).set(MDC_USER_ID, longUserId);
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldCallClearAllExactlyOncePerExchange() {

    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext, times(1)).clearAll();
  }

  @Test
  void shouldNotCallSetWhenHeaderIsZeroLengthString() {

    headers.set(HEADER_USER_ID, "");

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext, never()).set(anyString(), anyString());
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldMaintainCorrectOrderOfOperations() {

    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    var inOrder = inOrder(loggingContext, chain);
    inOrder.verify(loggingContext).set(MDC_USER_ID, userId);
    inOrder.verify(chain).filter(exchange);
    inOrder.verify(loggingContext).clearAll();
  }

  @Test
  void shouldHandleMultipleHeaderValues() {

    headers.add(HEADER_USER_ID, "user123");
    headers.add(HEADER_USER_ID, "user456"); // Second value should be ignored

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext).set(MDC_USER_ID, "user123"); // Should use first value
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldHandleEmptyHeaders() {
    // no headers set given at all

    // when
    Mono<Void> result = filter.filter(exchange, chain);

    // then
    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext, never()).set(anyString(), anyString());
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldWorkWithChainThatReturnsNonEmptyMono() {

    String userId = "user123";
    headers.set(HEADER_USER_ID, userId);
    when(chain.filter(exchange)).thenReturn(Mono.fromRunnable(() -> {
      // simulate some processing
    }));

    // when
    Mono<Void> result = filter.filter(exchange, chain);

    // then
    StepVerifier.create(result)
        .verifyComplete();

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(chain).filter(exchange);
    verify(loggingContext).clearAll();
  }
}