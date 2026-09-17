package com.incode.verification.adapter.in.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.result.VerificationResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VerificationControllerTest {
  private final GetVerificationUseCase retriever = mock(GetVerificationUseCase.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new VerificationController(retriever))
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @Test
  void retrievalDelegatesByVerificationId() throws Exception {
    var id = UUID.randomUUID();
    when(retriever.get(id)).thenReturn(view(id));

    mvc.perform(get("/verifications/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationId").value(id.toString()));
    verify(retriever).get(id);
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
