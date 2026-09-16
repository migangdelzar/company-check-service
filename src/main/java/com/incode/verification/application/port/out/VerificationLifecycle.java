package com.incode.verification.application.port.out;

import com.incode.verification.domain.aggregate.Verification;
import java.util.function.Consumer;

/** PostgreSQL-first lifecycle boundary: durable state is written before coordination work. */
public interface VerificationLifecycle {
  void start(Verification verification);

  void transition(Verification verification, Consumer<VerificationRepository> write);
}
