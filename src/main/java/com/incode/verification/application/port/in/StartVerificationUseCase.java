package com.incode.verification.application.port.in;

import com.incode.verification.application.port.out.VerificationView;

public interface StartVerificationUseCase {
    VerificationView start(StartVerificationCommand command);

    record StartVerificationCommand(String query) { }
}
