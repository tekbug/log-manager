package com.practices.loggingspringbootstarter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.practices.loggingspringbootstarter.config.CoreLoggingAutoConfiguration;
import com.practices.loggingspringbootstarter.config.WebLoggingAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({CoreLoggingAutoConfiguration.class, WebLoggingAutoConfiguration.class})
@DisplayName("MdcPopulatingFilter Integration Test")
class MdcPopulatingFilterTest {

  @Autowired private MockMvc mockMvc;

  @RestController
  static class TestController {

    @GetMapping("/test-mdc")
    public String getUserIdFromMdc() {
      return MDC.get("userId");
    }
  }

  @Test
  @DisplayName("should add userId to MDC when X-User-ID header is present")
  void shouldAddUserIdToMdcWhenHeaderIsPresent() throws Exception {
    mockMvc
        .perform(get("/test-mdc").header("X-User-ID", "user-456"))
        .andExpect(status().isOk())
        .andExpect(content().string("user-456"));
  }

  @Test
  @DisplayName("should not add userId to MDC when X-User-ID header is absent")
  void shouldNotAddUserIdToMdcWhenHeaderIsAbsent() throws Exception {
    mockMvc
        .perform(get("/test-mdc"))
        .andExpect(status().isOk())
        .andExpect(content().string("")); // Expect an empty string as MDC.get() returns null.
  }

  @Test
  @DisplayName("should clear MDC after request completion")
  void shouldClearMdcAfterRequest() throws Exception {
    // Pre-condition: Ensure MDC is clear before starting.
    MDC.clear();

    // Perform the request that populates the MDC.
    mockMvc.perform(get("/test-mdc").header("X-User-ID", "user-789"));

    // Post-condition: Assert that the filter's "finally" block has cleared the MDC.
    // This is the most crucial test for preventing context leaks.
    assertThat(MDC.get("userId")).isNull();
  }
}
