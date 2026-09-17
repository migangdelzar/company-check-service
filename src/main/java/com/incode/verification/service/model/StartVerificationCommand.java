package com.incode.verification.service.model;

import java.util.UUID;

public record StartVerificationCommand(UUID verificationId, String query) {}
