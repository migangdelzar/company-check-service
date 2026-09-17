package com.incode.verification.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.out.InboundRateLimiter;
import com.incode.verification.application.result.VerificationResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VerificationController.class)
class VerificationControllerSliceTest {
  @Autowired private MockMvc mvc;

  @MockitoBean private GetVerificationUseCase getVerification;

  @MockitoBean private InboundRateLimiter inboundRateLimiter;

  @Test
  void returnsNoStoreForVerificationReads() throws Exception {
    var id = UUID.randomUUID();
    when(getVerification.get(id))
        .thenReturn(
            new VerificationResult(
                id, "Acme", "acme", null, null, null, null, null, null, null));

    mvc.perform(get("/verifications/{verificationId}", id))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
  }
}
