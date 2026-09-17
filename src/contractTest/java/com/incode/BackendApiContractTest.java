package com.incode;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incode.verification.adapter.in.web.BackendServiceController;
import com.incode.verification.adapter.in.web.VerificationController;
import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.in.StartVerificationCommand;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import com.incode.verification.application.port.out.InboundRateLimiter;
import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.company.Company;
import com.incode.verification.domain.provider.ProviderType;
import com.incode.verification.domain.verification.VerificationStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({BackendServiceController.class, VerificationController.class})
class BackendApiContractTest {
  @Autowired private MockMvc mvc;

  @MockitoBean private GetVerificationUseCase getVerification;

  @MockitoBean private StartVerificationUseCase startVerification;

  @MockitoBean private InboundRateLimiter inboundRateLimiter;

  @BeforeEach
  void allowBackendRequests() {
    when(inboundRateLimiter.tryAcquire())
        .thenReturn(
            new InboundRateLimiter.Decision(
                InboundRateLimiter.Decision.Status.ALLOWED, Duration.ZERO));
  }

  @Test
  void startsVerificationWithStableResponseContract() throws Exception {
    var id = UUID.randomUUID();
    when(startVerification.start(new StartVerificationCommand(id, "CJQUNXGW")))
        .thenReturn(completedView(id));

    mvc.perform(
            get("/backend-service")
                .param("verificationId", id.toString())
                .param("query", "CJQUNXGW"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.verificationId").value(id.toString()))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.provider").value("FREE"))
        .andExpect(jsonPath("$.company.cin").value("CJQUNXGW"));

    verify(startVerification).start(new StartVerificationCommand(id, "CJQUNXGW"));
  }

  @Test
  void readsVerificationWithStableResponseContract() throws Exception {
    var id = UUID.randomUUID();
    when(getVerification.get(id)).thenReturn(completedView(id));

    mvc.perform(get("/verifications/{verificationId}", id))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.verificationId").value(id.toString()))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.company.cin").value("CJQUNXGW"));

    verify(getVerification).get(id);
  }

  @Test
  void rejectsBlankQueryWithProblemDetails() throws Exception {
    mvc.perform(
            get("/backend-service")
                .param("verificationId", UUID.randomUUID().toString())
                .param("query", " "))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.title").value("Invalid request"));
  }

  private VerificationResult completedView(UUID id) {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    return new VerificationResult(
        id,
        "CJQUNXGW",
        "cjq" + "unxgw",
        now,
        now.plusSeconds(600),
        VerificationStatus.COMPLETED,
        new Company("CJQUNXGW", "Example Company", LocalDate.of(2020, 1, 1), "Mexico", true),
        null,
        ProviderType.FREE,
        null);
  }
}
