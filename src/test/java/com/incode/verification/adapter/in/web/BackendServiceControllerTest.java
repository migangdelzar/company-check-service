package com.incode.verification.adapter.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.incode.verification.application.port.in.*;
import com.incode.verification.application.port.out.VerificationView;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BackendServiceControllerTest {
  private final StartVerificationUseCase starter = mock(StartVerificationUseCase.class);
  private final GetVerificationUseCase retriever = mock(GetVerificationUseCase.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new BackendServiceController(starter, retriever))
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @Test
  void lookupReturnsPdfCompatibleRepresentationAndNoCache() throws Exception {
    var id = UUID.randomUUID();
    when(starter.start(any())).thenReturn(view(id));

    mvc.perform(get("/backend-service").param("query", "Acme"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-cache"))
        .andExpect(jsonPath("$.verificationId").value(id.toString()))
        .andExpect(jsonPath("$.query").value("Acme"));
  }

  @Test
  void blankQueryUsesProblemDetails() throws Exception {
    mvc.perform(get("/backend-service").param("query", " "))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.title").value("Invalid request"));
  }

  @Test
  void retrievalDelegatesByVerificationId() throws Exception {
    var id = UUID.randomUUID();
    when(retriever.get(id)).thenReturn(view(id));
    mvc.perform(get("/backend-service/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationId").value(id.toString()));
    verify(retriever).get(id);
  }

  private VerificationView view(UUID id) {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    return new VerificationView(
        id,
        "Acme",
        "acme",
        now,
        now.plusSeconds(600),
        com.incode.verification.domain.type.VerificationStatus.IN_PROGRESS,
        null,
        null,
        null,
        null);
  }
}
