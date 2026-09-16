package com.incode.verification.application.port.in;

import java.time.Instant;

public interface ExpireVerificationsUseCase {
    int expire(Instant now, int batchSize);
}
