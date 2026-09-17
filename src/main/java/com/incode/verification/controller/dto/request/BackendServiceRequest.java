package com.incode.verification.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Query parameters accepted by the verification lookup endpoint. */
public record BackendServiceRequest(
    @NotNull UUID verificationId, @NotBlank @Size(max = 128) String query) {}
