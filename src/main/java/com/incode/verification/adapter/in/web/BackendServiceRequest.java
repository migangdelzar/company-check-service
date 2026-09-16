package com.incode.verification.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Query parameters accepted by the verification lookup endpoint. */
public record BackendServiceRequest(@NotBlank @Size(max = 255) String query) {}
