package com.practices.loggingspringbootstarter.web;

import com.practices.loggingspringbootstarter.core.LoggingContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;

@ExtendWith(MockitoExtension.class)
class MdcPopulatingFilterTest {

  private static final String HEADER_USER_ID = "X-User-ID";
  private static final String MDC_USER_ID = "userID";

  @Mock
  private LoggingContext loggingContext;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private MdcPopulatingFilter filter;

  @BeforeEach
  void setUp() {
    filter = new MdcPopulatingFilter(loggingContext);
  }

  @Test
  void shouldSetUserIdInLoggingContextWhenHeaderIsPresent() throws ServletException, IOException {
    String userId = "user123";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userId);

    filter.doFilterInternal(request, response, filterChain);

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "\t", "\n", " \t \n "})
  void shouldNotSetUserIdWhenHeaderIsNullEmptyOrBlank(String headerValue) throws ServletException, IOException {
    when(request.getHeader(HEADER_USER_ID)).thenReturn(headerValue);
    filter.doFilterInternal(request, response, filterChain);
    verify(loggingContext, never()).set(eq(MDC_USER_ID), any());
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldSetUserIdWhenHeaderHasWhitespaceButIsNotBlank() throws ServletException, IOException {
    String userId = " user123 ";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userId);
    filter.doFilterInternal(request, response, filterChain);
    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldAlwaysClearContextEvenWhenFilterChainThrowsServletException() throws ServletException, IOException {
    String userId = "user123";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userId);
    ServletException expectedException = new ServletException("Test exception");
    doThrow(expectedException).when(filterChain).doFilter(request, response);
    try {
      filter.doFilterInternal(request, response, filterChain);
    } catch (ServletException e) {
      // Expected exception
    }

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldAlwaysClearContextEvenWhenFilterChainThrowsIOException() throws ServletException, IOException {
    String userId = "user123";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userId);
    IOException expectedException = new IOException("Test exception");
    doThrow(expectedException).when(filterChain).doFilter(request, response);
    try {
      filter.doFilterInternal(request, response, filterChain);
    } catch (IOException e) {
      // Expected exception
    }

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldAlwaysClearContextEvenWhenFilterChainThrowsRuntimeException() throws ServletException, IOException {
    String userId = "user123";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userId);
    RuntimeException expectedException = new RuntimeException("Test exception");
    doThrow(expectedException).when(filterChain).doFilter(request, response);
    try {
      filter.doFilterInternal(request, response, filterChain);
    } catch (RuntimeException e) {
      // Expected exception
    }

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldAlwaysClearContextEvenWhenLoggingContextSetThrowsException() throws ServletException, IOException {
    String userId = "user123";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userId);
    RuntimeException expectedException = new RuntimeException("MDC set failed");
    doThrow(expectedException).when(loggingContext).set(MDC_USER_ID, userId);
    try {
      filter.doFilterInternal(request, response, filterChain);
    } catch (RuntimeException e) {
      // Expected exception
    }

    verify(loggingContext).set(MDC_USER_ID, userId);
    verify(filterChain, never()).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldHandleNullLoggingContextGracefully() {
    MdcPopulatingFilter filterWithNullContext = new MdcPopulatingFilter(null);
    NullPointerException exception = assertThrows(NullPointerException.class, () -> {
      filterWithNullContext.doFilterInternal(request, response, filterChain);
    });

    assertNotNull(exception);
  }

  @Test
  void shouldProcessMultipleRequestsIndependently() throws ServletException, IOException {
    String firstUserId = "user123";
    String secondUserId = "user456";

    when(request.getHeader(HEADER_USER_ID)).thenReturn(firstUserId);
    filter.doFilterInternal(request, response, filterChain);

    reset(loggingContext, filterChain);
    when(request.getHeader(HEADER_USER_ID)).thenReturn(secondUserId);

    filter.doFilterInternal(request, response, filterChain);

    verify(loggingContext).set(MDC_USER_ID, secondUserId);
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldHandleSpecialCharactersInUserId() throws ServletException, IOException {
    String userIdWithSpecialChars = "user@domain.com|special-chars_123";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userIdWithSpecialChars);

    filter.doFilterInternal(request, response, filterChain);

    verify(loggingContext).set(MDC_USER_ID, userIdWithSpecialChars);
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldHandleVeryLongUserId() throws ServletException, IOException {
    String longUserId = "a".repeat(1000); // Very long user ID
    when(request.getHeader(HEADER_USER_ID)).thenReturn(longUserId);
    filter.doFilterInternal(request, response, filterChain);

    verify(loggingContext).set(MDC_USER_ID, longUserId);
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldCallClearAllExactlyOncePerRequest() throws ServletException, IOException {
    String userId = "user123";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userId);
    filter.doFilterInternal(request, response, filterChain);
    verify(loggingContext, times(1)).clearAll();
  }

  @Test
  void shouldNotCallSetWhenHeaderIsZeroLengthString() throws ServletException, IOException {
    when(request.getHeader(HEADER_USER_ID)).thenReturn("");
    filter.doFilterInternal(request, response, filterChain);
    verify(loggingContext, never()).set(anyString(), anyString());
    verify(filterChain).doFilter(request, response);
    verify(loggingContext).clearAll();
  }

  @Test
  void shouldMaintainCorrectOrderOfOperations() throws ServletException, IOException {
    String userId = "user123";
    when(request.getHeader(HEADER_USER_ID)).thenReturn(userId);
    filter.doFilterInternal(request, response, filterChain);
    var inOrder = inOrder(loggingContext, filterChain);
    inOrder.verify(loggingContext).set(MDC_USER_ID, userId);
    inOrder.verify(filterChain).doFilter(request, response);
    inOrder.verify(loggingContext).clearAll();
  }
}