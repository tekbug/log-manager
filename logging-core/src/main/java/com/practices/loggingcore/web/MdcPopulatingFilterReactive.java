package com.practices.loggingcore.web;

import com.practices.loggingcore.core.LoggingContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * A reactive web filter that populates the logging context (MDC) with user information
 * extracted from request headers.
 * <p>
 * This filter runs with high precedence to ensure the logging context is available
 * for all later processing, including other filters and controllers. It reliably
 * clears the context at the end of the request lifecycle, regardless of whether the
 * request completes successfully or results in an error.
 */

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MdcPopulatingFilterReactive implements WebFilter {
  private static final String HEADER_USER_ID = "X-User-ID";
  private static final String MDC_USER_ID = "userID";

  private final LoggingContext loggingContext;

  public MdcPopulatingFilterReactive(LoggingContext loggingContext) {
    this.loggingContext = loggingContext;
  }

  /**
   * Intercepts the incoming request to populate the logging context.
   * <p>
   * It extracts the user ID from the {@code X-User-ID} header and sets it in the
   * {@link LoggingContext}. The context is populated at the beginning of the reactive
   * stream execution (upon subscription) and is guaranteed to be cleared when the stream
   * terminates (via success, error, or cancellation) using a {@code doFinally} operator.
   * This ensures that contextual information does not leak between requests.
   *
   * @param exchange the current server exchange, providing access to the request.
   * @param chain    the filter chain to pass control to the next filter.
   * @return a {@link Mono<Void>} that indicates when request processing is complete.
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return Mono.defer(() -> {
          String userId = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
          if (userId != null && !userId.isBlank()) {
            loggingContext.set(MDC_USER_ID, userId);
          }
          return chain.filter(exchange);
        })
        .doFinally(signalType -> loggingContext.clearAll());
  }
}

