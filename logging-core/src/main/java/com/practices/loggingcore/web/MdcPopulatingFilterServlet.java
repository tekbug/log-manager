package com.practices.loggingcore.web;

import com.practices.loggingcore.core.LoggingContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A servlet filter that adds context to the logging context for every incoming HTTP request
 * with common web-related attributes. This class only makes sure the filtering works with
 * Servlet (non-reactive) web apps.
 */

public class MdcPopulatingFilterServlet extends OncePerRequestFilter {

  private static final String HEADER_USER_ID = "X-User-ID";
  private static final String MDC_USER_ID = "userID";

  private final LoggingContext loggingContext;

  public MdcPopulatingFilterServlet(LoggingContext loggingContext) {
    this.loggingContext = loggingContext;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      final String userId = request.getHeader(HEADER_USER_ID);
      if (userId != null && !userId.isBlank()) {
        loggingContext.set(MDC_USER_ID, userId);
      }
      filterChain.doFilter(request, response);
    } finally {
      loggingContext.clearAll();
    }
  }
}
