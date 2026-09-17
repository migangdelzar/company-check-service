package com.incode.verification.adapter.in.web;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incode.verification.application.port.in.StartVerificationUseCase;
import com.incode.verification.application.result.VerificationResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BackendServiceControllerTest {
  private final StartVerificationUseCase starter = mock(StartVerificationUseCase.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(
              new BackendServiceController(starter))
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @Test
  void lookupReturnsPdfCompatibleRepresentationAndNoStore() throws Exception {
    var id = UUID.randomUUID();
    when(starter.start(any())).thenReturn(view(id));

    mvc.perform(
            get("/backend-service")
                .param("verificationId", id.toString())
                .param("query", "Acme"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.verificationId").value(id.toString()))
        .andExpect(jsonPath("$.query").value("Acme"));
  }

  @Test
  void blankQueryUsesProblemDetails() throws Exception {
    mvc.perform(
            get("/backend-service")
                .param("verificationId", UUID.randomUUID().toString())
                .param("query", " "))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.title").value("Invalid request"));
  }

  private VerificationResult view(UUID id) {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    return new VerificationResult(
        id,
        "Acme",
        "acme",
        now,
        now.plusSeconds(600),
        com.incode.verification.domain.verification.VerificationStatus.IN_PROGRESS,
        null,
        null,
        null,
        null);
  }
}
