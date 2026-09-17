package com.incode.verification.application.port.in;

import java.util.UUID;

public record StartVerificationCommand(UUID verificationId, String query) {}
