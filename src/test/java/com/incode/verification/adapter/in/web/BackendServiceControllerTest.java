package com.incode.verification.adapter.in.web;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incode.verification.adapter.out.observability.MicrometerTelemetryAdapter;
import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import com.incode.verification.application.port.out.VerificationView;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BackendServiceControllerTest {
  private final StartVerificationUseCase starter = mock(StartVerificationUseCase.class);
  private final GetVerificationUseCase retriever = mock(GetVerificationUseCase.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(
              new BackendServiceController(
                  starter, retriever, new MicrometerTelemetryAdapter(new SimpleMeterRegistry())))
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @Test
  void lookupReturnsPdfCompatibleRepresentationAndNoStore() throws Exception {
    var id = UUID.randomUUID();
    when(starter.start(any())).thenReturn(view(id));

    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/backend-service")
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
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/backend-service")
                .param("verificationId", UUID.randomUUID().toString())
                .param("query", " "))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.title").value("Invalid request"));
  }

  @Test
  void retrievalDelegatesByVerificationId() throws Exception {
    var id = UUID.randomUUID();
    when(retriever.get(id)).thenReturn(view(id));
    mvc.perform(get("/verifications/{id}", id))
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
